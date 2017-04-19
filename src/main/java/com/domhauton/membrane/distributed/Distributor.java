package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.appraisal.AppraisalLedger;
import com.domhauton.membrane.distributed.block.gen.BlockException;
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
import com.domhauton.membrane.network.NetworkException;
import com.domhauton.membrane.network.NetworkManager;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageException;
import com.domhauton.membrane.storage.BackupLedger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Minutes;

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
  private final static long BLOCK_SIZE_BYTES = 16L * 1024L * 1024L * 1024L; //16MB
  private final static int MAX_BLOCK_LIFETIME_WEEKS = 2;
  private final static int TIME_BEFORE_FIRST_BROADCAST_MINS = 10;
  private final static int TIME_BEFORE_FIRST_UPLOAD_MINS = 1;
  private final static int TIME_BETWEEN_EACH_UPLOAD_MINS = 2;
  private final Logger logger = LogManager.getLogger();
  private final ShardStorage localShardStorage;

  private final ShardStorage peerShardStorage;

  private final BlockLedger blockLedger;
  private final AppraisalLedger appraisalLedger;
  private final ContractStore contractStore;
  private final BackupLedger backupLedger;

  private NetworkManager networkManager;
  private final String key;

  private int contractLimit;
  private final ScheduledExecutorService executorService;

  public Distributor(Path basePath, BackupLedger backupLedger, ShardStorage localShardStorage, ShardStorage peerShardStorage, NetworkManager networkManager, int contractLimit) throws DistributorException {
    this.localShardStorage = localShardStorage;
    this.backupLedger = backupLedger;
    this.peerShardStorage = peerShardStorage;
    this.networkManager = networkManager;
    this.contractLimit = contractLimit;

    key = networkManager.getPrivateEncryptionKey();

    executorService = Executors.newSingleThreadScheduledExecutor();

    blockLedger = new BlockLedger(basePath);
    appraisalLedger = new AppraisalLedger(basePath);
    contractStore = new ContractStore(basePath);
  }

  private void distributeShards() {
    // Expire any old blocks.
    Set<String> allRequiredShards = backupLedger.getAllRequiredShards();
    blockLedger.expireAllUselessBlocks(allRequiredShards);

    // Generate a temporary shard -> peer lookup table.
    ShardPeerLookup shardPeerLookup = blockLedger.generateShardPeerLookup();
    allRequiredShards.forEach(x -> shardPeerLookup.addDistributedShard(x, Priority.Normal));

    // Generate a list of ranked peers
    List<String> connectedPeersByRank = getAvailablePeersSortedByRank();

    // Upload shards in lookup table to connected peers.
    uploadUndeployedShards(shardPeerLookup, connectedPeersByRank);

    // Remove any disused block Ledgers
    blockLedger.removeAllExcept(contractStore.getMyBlockIds());

    // Remove any unnecessaryShard peer blocks.
    removeUnnecessaryPeerBlocks();
  }

  private List<String> getAvailablePeersSortedByRank() {
    // This is expensive. Be careful.
    Map<String, Double> peerReliabilityMap = contractStore.getCurrentPeers().stream()
        .filter(this::isPeerConnected)
        .collect(Collectors.toMap(x -> x, appraisalLedger::getPeerRating));

    // Do not chain with above. Memoize reliability score!
    return peerReliabilityMap.entrySet().stream()
        .sorted(Comparator.comparingDouble(Map.Entry::getValue))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Send all possible shards to top X peers and wait a second and re-prompt upload if shards remain.
   *
   * @param shardPeerLookup      A lookup object correlating peers and shards.
   * @param connectedPeersByRank A list of available peers by their rank.
   */
  private void uploadUndeployedShards(ShardPeerLookup shardPeerLookup, List<String> connectedPeersByRank) {
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
    logger.info("Shard distribution complete. {} shards remain undistributed.", totalUndeployedShards.size());
  }

  private void removeUnnecessaryPeerBlocks() {
    Set<String> unnecessaryBlocks = peerShardStorage.listShardIds();
    unnecessaryBlocks.removeAll(contractStore.getPeerBlockIds());
    for (String unnecessaryShard : unnecessaryBlocks) {
      try {
        peerShardStorage.removeShard(unnecessaryShard);
      } catch (ShardStorageException e) {
        logger.debug("Could not remove unnecessary shard during cleanup.");
      }
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
      byte[] encryptedBlockData = blockProcessor.toEncryptedBytes(key);
      networkManager.uploadBlockToPeer(peerId, encryptedBlockData);
      blockLedger.addBlock(encryptedBlockData, uploadedSet, peerId, DateTime.now().plusWeeks(MAX_BLOCK_LIFETIME_WEEKS));
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

  private void removeMyBlock(String peer, String blockId) {
    blockLedger.removeBlock(blockId);
    contractStore.removeMyBlockId(peer, blockId);
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
      int peerAllowedInequality = contractStore.getPeerAllowedInequality(peerId);
      if (peerAllowedInequality < 1) {
        contractStore.setPeerAllowedInequality(peerId, 1);
      }
      sendContractUpdateToPeer(peerId);
    } else {
      logger.warn("Could not add new contract peer. Too many existing contracts. {} of {}. [{}]",
          contractedPeers, contractLimit, peerId);
      throw new DistributorException("Could not add new contract peer. Too many existing contracts.");
    }
  }

  @Override
  public void receiveBlock(String peerId, String blockId, byte[] data) {
    String generatedBlockId = BlockLedger.generateBlockId(data);
    if (generatedBlockId.equals(blockId)) {
      try {
        contractStore.addPeerBlockId(peerId, blockId);
        peerShardStorage.storeShard(blockId, data);
      } catch (ContractStoreException e) {
        logger.warn("Insufficient contactable space for peer block. Ignoring. {}", e.getMessage());
      } catch (ShardStorageException e) {
        logger.error("Failed to store peer block. Ignoring. {}", e.getMessage());
      }
    } else {
      logger.error("Ignoring corrupt block. PEER[{}] BLOCK[{}]", peerId, blockId);
    }
    // Inform peer of new block and trigger evidence building.
    sendContractUpdateToPeer(peerId);
  }

  @Override
  public Set<EvidenceRequest> processPeerContractUpdate(String peerId, DateTime dateTime, int permittedInequality, Set<String> blockIds) {
    // Is the given datetime within the last hour.
    if (Math.abs(Minutes.minutesBetween(dateTime, DateTime.now()).getMinutes()) < DateTimeConstants.MINUTES_PER_HOUR) {
      // Check if actually contracted peer
      if (getContractedPeers().contains(peerId) && !blockIds.isEmpty()) {
        // Register that contact was made with peer.
        contractStore.setMyAllowedInequality(peerId, permittedInequality);
        Set<String> myBlockIds = contractStore.getMyBlockIds(peerId);
        appraisalLedger.registerPeerContact(peerId, dateTime, myBlockIds.size());

        // First remove any blocks we expect the peer to have, that they do not and record this.
        Set<String> lostRecordedBlockIds = new HashSet<>(myBlockIds);
        lostRecordedBlockIds.removeAll(blockIds);
        lostRecordedBlockIds.forEach(blockId -> contractStore.removeMyBlockId(peerId, blockId));
        lostRecordedBlockIds.forEach(blockId -> appraisalLedger.registerLostBlock(peerId, dateTime, myBlockIds.size()));

        // Second request any blocks they say they have that we don't know about.
        Set<String> unexpectedBlocks = new HashSet<>(blockIds);
        unexpectedBlocks.removeAll(contractStore.getMyBlockIds(peerId));
        Set<EvidenceRequest> unexpectedBlockRequests = getUnexpectedEvidenceRequests(blockIds, myBlockIds);

        // Now deal with the actual expected blocks.
        Set<String> expectedBlocks = blockIds.stream().filter(myBlockIds::contains).collect(Collectors.toSet());
        Set<EvidenceRequest> expiredBlockRequests = generateExpiredBlockEvidenceRequests(peerId, dateTime, expectedBlocks);
        Set<String> alreadyConfirmedBlocks = appraisalLedger.getReportsRecieved(peerId, dateTime, myBlockIds.size());
        Set<EvidenceRequest> activeBlockRequests = getActiveBlockEvidenceRequests(dateTime, expectedBlocks, alreadyConfirmedBlocks);

        // Aggregate all generated evidence requests.
        Set<EvidenceRequest> returnEvidenceRequests = new HashSet<>(
            activeBlockRequests.size() +
                expiredBlockRequests.size() +
                unexpectedBlockRequests.size());

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
    } else {
      // Acknowledge that you received the request but don't ask for anything.
      return Collections.emptySet();
    }
  }

  private Set<EvidenceRequest> getActiveBlockEvidenceRequests(DateTime dateTime, Set<String> expectedBlocks, Set<String> alreadyConfirmedBlocks) {
    return expectedBlocks.stream()
        .map(blockId -> {
          try {
            // If we are missing a shard from one of the blocks.
            if (blockLedger.getBlockShardIds(blockId).stream().allMatch(localShardStorage::hasShard)) {
              return new EvidenceRequest(blockId, EvidenceType.COMPUTE_HASH, blockLedger.getBlockEvidenceSalt(blockId, dateTime));
            } else {
              return new EvidenceRequest(blockId, EvidenceType.SEND_BLOCK);
            }
          } catch (BlockLedgerException e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        // Do not resend shard requests for blocks to delete or compute hash for.
        .filter(evidenceRequest -> evidenceRequest.getEvidenceType() == EvidenceType.SEND_BLOCK ||
            !alreadyConfirmedBlocks.contains(evidenceRequest.getBlockId()))
        .collect(Collectors.toSet());
  }

  private Set<EvidenceRequest> getUnexpectedEvidenceRequests(Set<String> blockIds, Set<String> myBlockIds) {
    return blockIds.stream()
        .filter(blockId -> !myBlockIds.contains(blockId))
        .map(blockId -> new EvidenceRequest(blockId, EvidenceType.SEND_BLOCK))
        .collect(Collectors.toSet());
  }

  private Set<EvidenceRequest> generateExpiredBlockEvidenceRequests(String peerId, DateTime dateTime, Set<String> expectedBlocks) {
    return expectedBlocks.stream()
        .filter(blockId -> {
          try {
            return blockLedger.isBlockExpired(blockId, dateTime);
          } catch (BlockLedgerException e) {
            return false;
          }
        })
        .peek(blockId -> removeMyBlock(peerId, blockId))
        .map(blockId -> new EvidenceRequest(blockId, EvidenceType.DELETE_BLOCK))
        .collect(Collectors.toSet());
  }

  @Override
  public void processEvidenceResponse(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    // Ensure datetime is within the last hour
    if (Math.abs(Minutes.minutesBetween(dateTime, DateTime.now()).getMinutes()) < DateTimeConstants.MINUTES_PER_HOUR) {
      evidenceResponses.forEach(evidenceResponse -> confirmEvidence(peerId, dateTime, evidenceResponse));
    }
  }

  private void confirmEvidence(String peerId, DateTime dt, EvidenceResponse res) {
    try {
      switch (res.getEvidenceType()) {
        case SEND_BLOCK:
          processRequestedBlockEvidence(peerId, dt, res.getBlockId(), res.getResponse());
          break;
        case COMPUTE_HASH:
          processRequestedHashEvidence(peerId, dt, res.getBlockId(), res.getResponse());
          break;
        default:
          logger.debug("Could not verify evidence type: {}. Ignoring.", res.getEvidenceType());
          break;
      }
    } catch (BlockException e) {
      logger.error("Failed to process evidence. {}", e.getMessage());
    }
  }

  private void processRequestedBlockEvidence(String peerId, DateTime dt, String blockId, byte[] data) throws BlockException {
    BlockProcessor blockProcessor = new BlockProcessor(data, key);
    String actualHash = BlockLedger.generateBlockId(data);
    // Check not corrupted
    if (actualHash.equals(blockId)) {
      // This block actually exists, so force add it back into the ledger.
      contractStore.addMyBlockIdForce(peerId, actualHash);
      blockProcessor.getShardMap().forEach((String shardId, byte[] shardData) -> {
        try {
          localShardStorage.storeShard(shardId, shardData);
        } catch (ShardStorageException e) {
          logger.error("Failed to store recovered block. Forgetting. {}", e.getMessage());
        }
      });
      appraisalLedger.registerPeerContact(peerId, dt, contractStore.getMyBlockCount(peerId), blockId);
    }
  }

  private void processRequestedHashEvidence(String peerId, DateTime dt, String blockId, byte[] responseData) {
    try {
      boolean correctHash = blockLedger.confirmBlockHash(blockId, dt, new String(responseData));
      if (correctHash) {
        appraisalLedger.registerPeerContact(peerId, dt, contractStore.getMyBlockCount(peerId), blockId);
      } else {
        logger.info("Sent invalid hash for [{}]. {}", blockId);
      }
    } catch (BlockLedgerException e) {
      logger.error("Failed to verify evidence for [{}]. {}", blockId, e.getMessage());
    }
  }

  @Override
  public Set<EvidenceResponse> processEvidenceRequests(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    // Ensure datetime is within the last hour and peer is contracted
    if (Math.abs(Minutes.minutesBetween(dateTime, DateTime.now()).getMinutes()) < DateTimeConstants.MINUTES_PER_HOUR
        && getContractedPeers().contains(peerId)) {
      // Work through every request
      Set<String> peerBlockIds = contractStore.getPeerBlockIds(peerId);
      return evidenceRequests.stream()
          .filter(evidenceRequest -> peerBlockIds.contains(evidenceRequest.getBlockId()))
          .map((EvidenceRequest evidenceRequest) -> processEvidenceRequest(peerId, evidenceRequest))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    } else {
      return Collections.emptySet();
    }
  }

  private EvidenceResponse processEvidenceRequest(String peer, EvidenceRequest evidenceRequest) {
    try {
      switch (evidenceRequest.getEvidenceType()) {
        case SEND_BLOCK:
          byte[] blockBytes = peerShardStorage.retrieveShard(evidenceRequest.getBlockId());
          return new EvidenceResponse(evidenceRequest.getBlockId(), EvidenceType.SEND_BLOCK, blockBytes);
        case COMPUTE_HASH:
          byte[] hashBytes = peerShardStorage.retrieveShard(evidenceRequest.getBlockId());
          byte[] saltedHash = BlockLedger.getSaltedHash(evidenceRequest.getSalt(), hashBytes).getBytes();
          return new EvidenceResponse(evidenceRequest.getBlockId(), EvidenceType.SEND_BLOCK, saltedHash);
        case DELETE_BLOCK:
          contractStore.removePeerBlockId(peer, evidenceRequest.getBlockId());
          peerShardStorage.removeShard(evidenceRequest.getBlockId());
          return null;
        default:
          logger.error("Could not find match for Evidence Type: {}. Ignoring.`", evidenceRequest.getEvidenceType());
          return null;
      }
    } catch (ShardStorageException | ContractStoreException e) {
      return null;
    }
  }

  @Override
  public void run() {
    executorService.scheduleAtFixedRate(this::sendUpdateToAllContractedPeers,
        TIME_BEFORE_FIRST_BROADCAST_MINS,
        DateTimeConstants.MINUTES_PER_HOUR,
        TimeUnit.MINUTES);

    executorService.scheduleAtFixedRate(this::distributeShards,
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
