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
import com.domhauton.membrane.storage.StorageManagerException;
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
public class ContractManagerImpl implements Runnable, Closeable, ContractManager {
  private final static long BLOCK_SIZE_BYTES = 24L * 1024L * 1024L * 1024L; //24MB
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

  public ContractManagerImpl(Path basePath, BackupLedger backupLedger, ShardStorage localShardStorage, ShardStorage peerShardStorage, NetworkManager networkManager, int contractLimit) throws ContractManagerException {
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

  void distributeShards() {
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

    // Remove unnecessary peers
    contractStore.removeUselessPeers();

    // Remove any unnecessaryShard peer blocks.
    removeUnnecessaryPeerBlocks();
  }

  private List<String> getAvailablePeersSortedByRank() {
    // This is expensive. Be careful.
    Map<String, Double> peerReliabilityMap = contractStore.getCurrentPeers().stream()
        .collect(Collectors.toMap(x -> x, appraisalLedger::getPeerRating));

    double mean = peerReliabilityMap.values().stream()
        .mapToDouble(x -> x)
        .average()
        .orElse(0.0d);
    double var = peerReliabilityMap.values().stream()
        .mapToDouble(x -> (x - mean))
        .map(x -> x * x)
        .average()
        .orElse(0.0);
    double sd = Math.sqrt(var);
    double minPermitted = mean - (1.5 * sd);

    peerReliabilityMap.entrySet().stream()
        .filter(peerRel -> peerRel.getValue() < minPermitted)
        .forEach(peerRel -> contractStore.setPeerAllowedInequality(peerRel.getKey(), 0));

    // Do not chain with above. Memoize reliability score!
    return contractStore.getCurrentPeers().stream()
        .filter(this::isPeerConnected)
        .sorted(Comparator.comparingDouble(x -> peerReliabilityMap.getOrDefault(x, 0.0)))
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
      int numberOfBlocksToUpload = 0;
      try {
        numberOfBlocksToUpload = contractStore.getMyBlockSpace(peerId);
      } catch (ContractStoreException e) {
        logger.warn("Peer contract removed mid-upload. Ignoring peer.");
      }
      // Cannot queue buffer more than 24*6 * 4/3 (base64) = 160MB of data safely
      // Actual limit 256MB but this leaves headroom for overheads
      numberOfBlocksToUpload = Math.min(5, numberOfBlocksToUpload);

      // If 0 - ignore this peer for now, they can't take more data. Continue onto next one.

      if (numberOfBlocksToUpload > 0) {
        // Start uploading them.
        for (int i = 0; i < numberOfBlocksToUpload && !shardsToUploadForPeer.isEmpty(); i++) {
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
    for (String shardHash : candidateShards) {
      try {
        if (blockSizeRemaining > 0 && blockSizeRemaining > localShardStorage.getShardSize(shardHash)) { // Skip shard if too large
          byte[] shardData = localShardStorage.retrieveShard(shardHash);
          // Use (returned) compressed size for calculation
          long compressedShardSize = blockProcessor.addLocalShard(shardHash, shardData);
          blockProcessor.addFileHistory(new HashSet<>(backupLedger.getAllRelatedJournalEntries(shardHash)));
          // Update loop variants
          blockSizeRemaining -= compressedShardSize;
          uploadedSet.add(shardHash);
        }
      } catch (ShardStorageException e) {
        logger.warn("Shard queued for upload but not in local shard storage. Possibly removed or damaged. [{}]", e.getMessage());
      }
    }

    // Do not upload if empty
    if (uploadedSet.isEmpty()) {
      logger.info("Could not fit any shards in block to peer [{}]", uploadedSet.size(), peerId);
      return uploadedSet;
    } else {
      // Upload the created block
      try {
        logger.info("Sending {} shards to [{}]", uploadedSet.size(), peerId);
        byte[] encryptedBlockData = blockProcessor.toEncryptedBytes(key);
        String blockId = blockLedger.addBlock(encryptedBlockData, uploadedSet, peerId, DateTime.now().plusWeeks(MAX_BLOCK_LIFETIME_WEEKS));
        networkManager.uploadBlockToPeer(peerId, blockId, encryptedBlockData);
        contractStore.addMyBlockId(peerId, blockId);
        return uploadedSet;
      } catch (ContractManagerException | NetworkException e) {
        logger.warn("Unable to upload generated block: {}", e.getMessage());
        return Collections.emptySet();
      }
    }
  }

  /**
   * Send updates to every contracted peer who is connected.
   */
  void sendUpdateToAllContractedPeers() {
    getContractedPeers().stream()
        .filter(networkManager::peerConnected)
        .forEach(this::sendContractUpdateToPeer);
  }

  /**
   * Send an update with currently held blocks and allowed inequality to peer.
   *
   * @param peerId Peer to send update to.
   */
  void sendContractUpdateToPeer(String peerId) {
    try {
      networkManager.sendContractUpdateToPeer(peerId,
          DateTime.now().hourOfDay().roundFloorCopy(),
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
  public synchronized void addContractedPeer(String peerId) throws ContractManagerException {
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
      throw new ContractManagerException("Could not add new contract peer. Too many existing contracts.");
    }
  }

  @Override
  public void receiveBlock(String peerId, String blockId, byte[] data) {
    String generatedBlockId = BlockLedger.generateBlockId(data);
    if (generatedBlockId.equals(blockId) || getContractedPeers().contains(peerId)) {
      try {
        contractStore.addPeerBlockId(peerId, blockId);
        peerShardStorage.storeShard(blockId, data);
      } catch (ContractStoreException e) {
        logger.error("Insufficient contactable space for peer block. Ignoring. {}", e.getMessage());
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
    permittedInequality = Math.max(0, permittedInequality);
    logger.info("Received peer contract update from [{}]. {} blocks stored. {} permitted inequality.",
        peerId, blockIds.size(), permittedInequality);
    // Is the given datetime within the last hour.
    if (Math.abs(Minutes.minutesBetween(dateTime, DateTime.now()).getMinutes()) < DateTimeConstants.MINUTES_PER_HOUR) {
      // Check if actually contracted peer
      if (getContractedPeers().contains(peerId) && !blockIds.isEmpty()) {
        // Register that contact was made with peer.
        contractStore.setMyAllowedInequality(peerId, permittedInequality);
        Set<String> myBlockIds = contractStore.getMyBlockIds(peerId);

        // First remove any blocks we expect the peer to have, that they do not and record this.
        Set<String> lostRecordedBlockIds = new HashSet<>(myBlockIds);
        lostRecordedBlockIds.removeAll(blockIds);
        lostRecordedBlockIds.forEach(blockId -> contractStore.removeMyBlockId(peerId, blockId));
        lostRecordedBlockIds.forEach(blockId -> appraisalLedger.registerLostBlock(peerId, dateTime, myBlockIds.size()));

        // Second request any blocks they say they have that we don't know about.
        Set<String> unexpectedBlocks = new HashSet<>(blockIds);
        unexpectedBlocks.removeAll(contractStore.getMyBlockIds(peerId));
        Set<EvidenceRequest> unexpectedBlockRequests = getUnexpectedEvidenceRequests(blockIds, myBlockIds);
        unexpectedBlockRequests.forEach(x -> contractStore.addMyBlockIdForce(peerId, x.getBlockId()));

        appraisalLedger.registerPeerContact(peerId, dateTime, myBlockIds.size());

        // Now deal with the actual expected blocks.
        Set<String> expectedBlocks = blockIds.stream()
            .filter(myBlockIds::contains)
            .collect(Collectors.toSet());
        Set<String> blocksToSkip = appraisalLedger.getReportsRecieved(peerId, dateTime, myBlockIds.size());
        Set<EvidenceRequest> activeBlockRequests = getActiveBlockEvidenceRequests(dateTime, expectedBlocks, blocksToSkip);

        // Aggregate all generated evidence requests.
        Set<EvidenceRequest> returnEvidenceRequests = new HashSet<>(
            activeBlockRequests.size() +
                unexpectedBlockRequests.size());

        returnEvidenceRequests.addAll(activeBlockRequests);
        returnEvidenceRequests.addAll(unexpectedBlockRequests);

        logger.info("Contract update complete. {} blocks lost by peer. {} unexpected blocks. {} active blocks. {} evidence requests sent.",
            lostRecordedBlockIds.size(),
            unexpectedBlockRequests.size(),
            expectedBlocks.size(),
            returnEvidenceRequests.size());

        // Remove any blocks deleted!
        returnEvidenceRequests.stream()
            .filter(x -> x.getEvidenceType() == EvidenceType.DELETE_BLOCK)
            .forEach(x -> contractStore.removeMyBlockId(peerId, x.getBlockId()));

        return returnEvidenceRequests;
      } else {
        // Record that the peer was around at this time and nothing else. May be selected for a contract one day.
        appraisalLedger.registerPeerContact(peerId, dateTime, 0);
        // Acknowledge that you received the request but don't ask for anything.
        return Collections.emptySet();
      }
    } else {
      logger.info("Ignoring contract update from [{}]. Not contracted and no blocks!", peerId);
      // Acknowledge that you received the request but don't ask for anything.
      return Collections.emptySet();
    }
  }

  private Set<EvidenceRequest> getActiveBlockEvidenceRequests(DateTime dateTime, Set<String> expectedBlocks, Set<String> skipBlocks) {
    return expectedBlocks.stream()
        .map(blockId -> activeBlockId2EvidenceRequest(dateTime, blockId))
        .filter(evidenceRequest -> evidenceRequest.getEvidenceType() == EvidenceType.SEND_BLOCK ||
            !skipBlocks.contains(evidenceRequest.getBlockId()))
        .collect(Collectors.toSet());
  }

  private EvidenceRequest activeBlockId2EvidenceRequest(DateTime dateTime, String blockId) {
    try {
      if (blockLedger.isBlockExpired(blockId, dateTime)) {
        return new EvidenceRequest(blockId, EvidenceType.DELETE_BLOCK);
      } else {
        // If we are missing a shard from one of the blocks.
        if (blockLedger.getBlockShardIds(blockId).stream().allMatch(localShardStorage::hasShard)) {
          return new EvidenceRequest(blockId, EvidenceType.COMPUTE_HASH, blockLedger.getBlockEvidenceSalt(blockId, dateTime));
        } else {
          return new EvidenceRequest(blockId, EvidenceType.SEND_BLOCK);
        }
      }
    } catch (BlockLedgerException e) {
      return new EvidenceRequest(blockId, EvidenceType.DELETE_BLOCK);
    }
  }

  private Set<EvidenceRequest> getUnexpectedEvidenceRequests(Set<String> blockIds, Set<String> myBlockIds) {
    return blockIds.stream()
        .filter(blockId -> !myBlockIds.contains(blockId))
        .map(blockId -> new EvidenceRequest(blockId, EvidenceType.SEND_BLOCK))
        .collect(Collectors.toSet());
  }

  @Override
  public void processEvidenceResponses(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
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
    logger.info("Processing block evidence for block [{}]", blockId);
    String actualBlockId = BlockLedger.generateBlockId(data);
    // Check not corrupted
    if (actualBlockId.equals(blockId)) {
      // This block actually exists, so force add it back into the ledger.
      contractStore.addMyBlockIdForce(peerId, actualBlockId);
      logger.info("Recovering {} individual shards", blockProcessor.getShardCount());
      blockProcessor.getShardMap().forEach((String shardId, byte[] shardData) -> {
        try {
          logger.info("Recovering shard [{}] from block [{}]", shardId, blockId);
          localShardStorage.storeShard(shardId, shardData);
        } catch (ShardStorageException e) {
          logger.error("Failed to store recovered block. Forgetting. {}", e.getMessage());
        }
      });
      for (String fileHistEntry : blockProcessor.getFileHistory()) {
        try {
          backupLedger.insertJournalEntry(fileHistEntry);
        } catch (StorageManagerException e) {
          logger.error("Failed to insert recovered file history. [{}] Forgetting. {}",
              fileHistEntry, e.getMessage());
        }
      }
      appraisalLedger.registerPeerContact(peerId, dt, contractStore.getMyBlockCount(peerId), blockId);
    } else {
      logger.warn("Received block ID does not match stated block ID! Given: [{}] Actual: [{}]", blockId, actualBlockId);
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
    logger.info("Received {} evidence requests from [{}] for [{}]", evidenceRequests.size(), peerId, dateTime);
    // Ensure datetime is within the last hour and peer is contracted
    if (Math.abs(Minutes.minutesBetween(dateTime, DateTime.now()).getMinutes()) < DateTimeConstants.MINUTES_PER_HOUR) {
      logger.debug("Processing {} evidence requests from [{}]", evidenceRequests.size(), peerId);
      // Work through every request
      Set<String> peerBlockIds = contractStore.getPeerBlockIds(peerId);
      return evidenceRequests.stream()
          .filter(evidenceRequest -> peerBlockIds.contains(evidenceRequest.getBlockId()))
          .map((EvidenceRequest evidenceRequest) -> processEvidenceRequest(peerId, evidenceRequest))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    } else {
      logger.warn("Ignoring {} evidence requests from [{}]. Too Late.", evidenceRequests.size(), peerId);
      return Collections.emptySet();
    }
  }

  EvidenceResponse processEvidenceRequest(String peer, EvidenceRequest evidenceRequest) {
    try {
      switch (evidenceRequest.getEvidenceType()) {
        case SEND_BLOCK:
          logger.debug("Processing send block evidence request for block [{}]", evidenceRequest.getBlockId());
          byte[] blockBytes = peerShardStorage.retrieveShard(evidenceRequest.getBlockId());
          return new EvidenceResponse(evidenceRequest.getBlockId(), evidenceRequest.getEvidenceType(), blockBytes);
        case COMPUTE_HASH:
          logger.debug("Processing salted hash evidence request for block [{}]", evidenceRequest.getBlockId());
          byte[] hashBytes = peerShardStorage.retrieveShard(evidenceRequest.getBlockId());
          byte[] saltedHash = BlockLedger.getSaltedHash(evidenceRequest.getSalt(), hashBytes).getBytes();
          return new EvidenceResponse(evidenceRequest.getBlockId(), evidenceRequest.getEvidenceType(), saltedHash);
        case DELETE_BLOCK:
          logger.debug("Processing delete request for block [{}]", evidenceRequest.getBlockId());
          contractStore.removePeerBlockId(peer, evidenceRequest.getBlockId());
          peerShardStorage.removeShard(evidenceRequest.getBlockId());
          return null;
        default:
          logger.error("Could not find match for Evidence Type: {}. Ignoring.", evidenceRequest.getEvidenceType());
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
