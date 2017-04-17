package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.appraisal.AppraisalLedger;
import com.domhauton.membrane.distributed.block.gen.BlockProcessor;
import com.domhauton.membrane.distributed.block.ledger.BlockLedger;
import com.domhauton.membrane.distributed.block.ledger.BlockLedgerException;
import com.domhauton.membrane.distributed.block.manifest.Priority;
import com.domhauton.membrane.distributed.block.manifest.ShardPeerLookup;
import com.domhauton.membrane.distributed.contract.ContractStore;
import com.domhauton.membrane.distributed.contract.ContractStoreException;
import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import com.domhauton.membrane.distributed.evidence.EvidenceType;
import com.domhauton.membrane.distributed.util.RateLimiter;
import com.domhauton.membrane.network.NetworkException;
import com.domhauton.membrane.network.NetworkManager;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class Distributor implements Runnable, Closeable, ContractManager {
  private final static Duration UPLOAD_RATE_LIMIT = Duration.standardSeconds(5);
  private final static long BLOCK_SIZE_BYTES = 16L * 1024L * 1024L * 1024L; //16MB
  private final static int MAX_BLOCK_LIFETIME_WEEKS = 2;
  private final static int TIME_BEFORE_FIRST_BROADCAST_MINS = 10;
  private final static int TIME_BEFORE_FIRST_UPLOAD_MINS = 1;
  private final static int TIME_BETWEEN_EACH_UPLOAD_MINS = 2;
  private final Logger logger = LogManager.getLogger();

  private final ShardStorage localShardStorage;

  private final ShardStorage peerShardStorage;
  private final ShardPeerLookup shardPeerLookup;
  private final BlockLedger blockLedger;
  private final AppraisalLedger appraisalLedger;
  private final RateLimiter uploadRateLimiter;

  private final ContractStore contractStore;

  private NetworkManager networkManager;

  private int contractLimit;
  private final ScheduledExecutorService executorService;

  public Distributor(Path basePath, ShardStorage localShardStorage, ShardStorage peerShardStorage, NetworkManager networkManager, int contractLimit) throws DistributorException {
    this.localShardStorage = localShardStorage;
    this.peerShardStorage = peerShardStorage;
    this.networkManager = networkManager;
    this.contractLimit = contractLimit;

    executorService = Executors.newSingleThreadScheduledExecutor();

    blockLedger = new BlockLedger(basePath);
    appraisalLedger = new AppraisalLedger(basePath);
    contractStore = new ContractStore(basePath);

    shardPeerLookup = blockLedger.generateShardPeerLookup();
    uploadRateLimiter = new RateLimiter(this::beginUpload, UPLOAD_RATE_LIMIT);
  }

  public void storeShard(String md5Hash, Priority priority) {
    shardPeerLookup.addDistributedShard(md5Hash, priority);
    uploadRateLimiter.schedule();
  }

  void beginUpload() {
    // This is expensive. Be careful.
    Map<String, Double> peerReliabilityMap = contractStore.getCurrentPeers().stream()
        .filter(this::isPeerConnected)
        .collect(Collectors.toMap(x -> x, appraisalLedger::getPeerRating));
    // Do not chain with above. Memoize reliability score!
    List<String> connectedPeersByRank = peerReliabilityMap.entrySet().stream()
        .sorted(Comparator.comparingDouble(Map.Entry::getValue))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    // Send all possible shards to top X peers and wait a second and re-prompt upload if shards remain.

    Set<String> usedShardSet = new HashSet<>();
    for (String peerId : connectedPeersByRank) {
      // What shards can this peer get?
      Set<String> shardsToUploadForPeer = shardPeerLookup.undeployedShards(peerId);
      shardsToUploadForPeer.removeAll(usedShardSet); // Do NOT duplicate upload any shards within one round

      // How much will peer store for us?
      int numberOfShardsToUpload = 0;
      try {
        numberOfShardsToUpload = contractStore.getMyBlockSpace(peerId);
      } catch (ContractStoreException e) {
        logger.warn("Peer contract removed mid-upload. Ignoring peer.");
      }

      // If 0 - ignore this peer for now, they can't take more data. Continue onto next one.

      if (numberOfShardsToUpload > 0) {
        // Start uploading them.
        for (int i = 0; i < numberOfShardsToUpload && !shardsToUploadForPeer.isEmpty(); i++) {
          Set<String> uploadedShards = uploadShardsToPeer(peerId, shardsToUploadForPeer);
          if (uploadedShards.isEmpty()) { // If upload was useless, there is no more to upload.
            shardsToUploadForPeer.clear();
          } else {
            shardsToUploadForPeer.removeAll(uploadedShards);
          }
        }
        usedShardSet.addAll(shardsToUploadForPeer);
      }
    }

    Set<String> totalUndeployedShards = shardPeerLookup.undeployedShards();
    // If there exist shards with no potential peers schedule peer search
    if (totalUndeployedShards.size() - usedShardSet.size() > 0) {
      // Schedule new scan
      //TODO
    }
  }

  private boolean isPeerConnected(String peerId) {
    return networkManager.peerConnected(peerId);
  }

  private Set<String> uploadShardsToPeer(String peerId, Set<String> candidateShards) {
    Set<String> uploadedSet = new HashSet<>();

    // Prepare block for filling
    BlockProcessor blockProcessor = new BlockProcessor();
    long blockSizeRemaining = BLOCK_SIZE_BYTES;

    // Fill block
    Iterator<String> shardQueue = candidateShards.iterator();
    for (String shardHash = shardQueue.next(); shardQueue.hasNext() && blockSizeRemaining > 0; shardHash = shardQueue.next()) {
      try {
        long shardDataLength = localShardStorage.getShardSize(shardHash);
        if (blockSizeRemaining < shardDataLength) { // Skip shard if too large
          byte[] shardData = localShardStorage.retrieveShard(shardHash);
          // Use (returned) compressed size for calculation
          long compressedShardSize = blockProcessor.addLocalShard(shardHash, shardData);
          // Update loop variants
          blockSizeRemaining -= compressedShardSize;
          uploadedSet.add(shardHash);
        }
      } catch (ShardStorageException e) {
        logger.warn("Shard queued for upload but not in local shard storage. Possibly removed or damaged. [{}]", e.getMessage());
      }
    }

    // Upload the created block
    try {
      byte[] encryptedBlockData = blockProcessor.toEncryptedBytes();
      networkManager.uploadBlockToPeer(peerId, encryptedBlockData);
      blockLedger.addBlock(encryptedBlockData, uploadedSet, peerId, DateTime.now().plusWeeks(MAX_BLOCK_LIFETIME_WEEKS));
      uploadedSet.forEach(shardId -> shardPeerLookup.addStoragePeer(shardId, peerId));
      return uploadedSet;
    } catch (DistributorException | NetworkException e) {
      logger.warn("Unable to upload generated block: {}", e.getMessage());
      return Collections.emptySet();
    }
  }

  /**
   * Send updates to every contracted peer who is connected.
   */
  private void sendUpdateToAllContractedPeers() {
    getContractedPeers().stream()
        .filter(networkManager::peerConnected)
        .forEach(this::sendContractUpdateToPeer);
  }

  private void sendContractUpdateToPeer(String peerId) {
    try {
      networkManager.sendContractUpdateToPeer(peerId,
          contractStore.getPeerAllowedInequality(peerId),
          contractStore.getPeerBlockIds(peerId));
    } catch (NetworkException e) {
      logger.warn("Failed to send contract update to contracted peer. [{}]", peerId);
    }
  }

  @Override
  public Set<String> getContractedPeers() {
    return contractStore.getCurrentPeers();
  }

  @Override
  public int getContractCountTarget() {
    return contractLimit;
  }

  @Override
  public synchronized void addContractedPeer(String peerId) throws DistributorException {
    int contractedPeers = getContractedPeers().size();
    if (contractedPeers < contractLimit) {
      // Set to base allowed inequality.
      contractStore.setPeerAllowedInequality(peerId, 1);
      sendContractUpdateToPeer(peerId);
    } else {
      logger.warn("Could not add new contract peer. Too many existing contracts. {} of {}. [{}]",
          contractedPeers, contractLimit, peerId);
      throw new DistributorException("Could not add new contract peer. Too many existing contracts.");
    }
  }

  @Override
  public Set<EvidenceRequest> processPeerContractUpdate(String peerId, DateTime dateTime, int permittedInequality, Set<String> blockIds) {
    // Check if actually contracted peer
    if (getContractedPeers().contains(peerId) && !blockIds.isEmpty()) {
      // Register that contact was made with peer.
      contractStore.setMyAllowedInequality(peerId, permittedInequality);
      Set<String> myBlockIds = contractStore.getMyBlockIds(peerId);
      appraisalLedger.registerPeerContact(peerId, dateTime, myBlockIds.size());

      // First remove any blocks we expect the peer to have, that they do not.
      Set<String> lostRecordedBlockIds = new HashSet<>(myBlockIds);
      lostRecordedBlockIds.removeAll(blockIds);
      lostRecordedBlockIds.forEach(blockId -> contractStore.removeMyBlockId(peerId, blockId));

      // Second request any blocks they say they have that we don't know about.
      Set<String> unexpectedBlocks = new HashSet<>(blockIds);
      unexpectedBlocks.removeAll(contractStore.getMyBlockIds(peerId));
      Set<EvidenceRequest> unexpectedBlockRequests = blockIds.stream()
          .filter(blockId -> !myBlockIds.contains(blockId))
          .map(blockId -> new EvidenceRequest(blockId, EvidenceType.SEND_BLOCK))
          .collect(Collectors.toSet());

      // Now deal with the actual expected blocks.
      Set<String> expectedBlocks = blockIds.stream().filter(myBlockIds::contains).collect(Collectors.toSet());

      // Deal with expected expired blocks
      Set<EvidenceRequest> expiredBlockRequests = expectedBlocks.stream()
          .filter(blockId -> {
            try {
              return blockLedger.isBlockExpired(blockId, dateTime);
            } catch (BlockLedgerException e) {
              return false;
            }
          })
          .peek(blockLedger::removeBlock)
          .peek(blockId -> contractStore.removeMyBlockId(peerId, blockId))
          .map(blockId -> new EvidenceRequest(blockId, EvidenceType.DELETE_BLOCK))
          .collect(Collectors.toSet());

      // Deal with active blocks
      Set<EvidenceRequest> activeBlockRequests = expectedBlocks.stream()
          .map(blockId -> {
            try {
              return new EvidenceRequest(blockId, EvidenceType.COMPUTE_HASH, blockLedger.getBlockEvidenceSalt(blockId, dateTime));
            } catch (BlockLedgerException e) {
              return null;
            }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      // Aggregate all generated evidence requests.
      Set<EvidenceRequest> returnEvidenceRequests = new HashSet<>(activeBlockRequests.size() + expiredBlockRequests.size() + unexpectedBlockRequests.size());
      returnEvidenceRequests.addAll(activeBlockRequests);
      returnEvidenceRequests.addAll(expiredBlockRequests);
      returnEvidenceRequests.addAll(unexpectedBlockRequests);

      return returnEvidenceRequests;
    } else {
      // Record that the peer was around at this time and nothing else. May be selected for a contract one day.
      appraisalLedger.registerPeerContact(peerId, dateTime, 0);
      // Acknowledge that you received the request but don't ask for anything.
      return Collections.emptySet();
    }
  }

  @Override
  public void processEvidenceResponse(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    //FIXME Implement
  }

  @Override
  public Set<EvidenceResponse> processEvidenceRequests(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    //FIXME Implement
    return Collections.emptySet();
  }

  @Override
  public void run() {
    executorService.scheduleAtFixedRate(this::sendUpdateToAllContractedPeers,
        TIME_BEFORE_FIRST_BROADCAST_MINS,
        DateTimeConstants.MINUTES_PER_HOUR,
        TimeUnit.MINUTES);

    executorService.scheduleAtFixedRate(this::beginUpload,
        TIME_BEFORE_FIRST_UPLOAD_MINS,
        TIME_BETWEEN_EACH_UPLOAD_MINS,
        TimeUnit.MINUTES);

    appraisalLedger.run();
    blockLedger.run();
    contractStore.run();
  }

  @Override
  public void close() {
    executorService.shutdown();

    appraisalLedger.close();
    blockLedger.close();
    contractStore.close();
  }
}
