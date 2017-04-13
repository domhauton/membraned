package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.appraisal.AppraisalLedger;
import com.domhauton.membrane.distributed.block.gen.BlockProcessor;
import com.domhauton.membrane.distributed.block.ledger.BlockLedger;
import com.domhauton.membrane.distributed.block.manifest.Priority;
import com.domhauton.membrane.distributed.block.manifest.ShardPeerLookup;
import com.domhauton.membrane.distributed.contract.ContractStore;
import com.domhauton.membrane.distributed.contract.ContractStoreException;
import com.domhauton.membrane.distributed.util.RateLimiter;
import com.domhauton.membrane.network.NetworkException;
import com.domhauton.membrane.network.NetworkManager;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class Distributor implements Runnable, Closeable, ContractManager {
  private final static Duration UPLOAD_RATE_LIMIT = Duration.standardSeconds(5);
  private final static long BLOCK_SIZE_BYTES = 16L * 1024L * 1024L * 1024L; //16MB
  private final static int MAX_BLOCK_LIFETIME = 2;
  private final Logger logger = LogManager.getLogger();

  private final ShardStorage shardStorage;

  private final ShardPeerLookup shardPeerLookup;
  private final BlockLedger blockLedger;
  private final AppraisalLedger appraisalLedger;
  private final RateLimiter uploadRateLimiter;
  private final ContractStore contractStore;

  private NetworkManager networkManager;

  private int contractLimit;

  public Distributor(Path basePath, ShardStorage shardStorage, NetworkManager networkManager, int contractLimit) throws DistributorException {
    this.shardStorage = shardStorage;
    this.networkManager = networkManager;
    this.contractLimit = contractLimit;

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

  public void retrieveShards() {

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
        long shardDataLength = shardStorage.getShardSize(shardHash);
        if (blockSizeRemaining < shardDataLength) { // Skip shard if too large
          byte[] shardData = shardStorage.retrieveShard(shardHash);
          // Use (returned) compressed size for calculation
          long compressedShardSize = blockProcessor.addLocalShard(shardHash, shardData);
          // Update loop variants
          blockSizeRemaining -= compressedShardSize;
          uploadedSet.add(shardHash);
        }
      } catch (ShardStorageException e) {
        logger.warn("Shard queued for upload but not in local shard storage. Requesting recovery. Possibly removed or damaged.");
        requestShardRecovery(shardHash);
      }
    }

    // Upload the created block
    try {
      byte[] encryptedBlockData = blockProcessor.toEncryptedBytes();
      networkManager.uploadBlockToPeer(peerId, encryptedBlockData);
      blockLedger.addBlock(encryptedBlockData, uploadedSet, peerId, DateTime.now().plusWeeks(MAX_BLOCK_LIFETIME));
      uploadedSet.forEach(shardId -> shardPeerLookup.addStoragePeer(shardId, peerId));
      return uploadedSet;
    } catch (DistributorException | NetworkException e) {
      logger.warn("Unable to upload generated block: {}", e.getMessage());
      return Collections.emptySet();
    }
  }

  public void requestShardRecovery(String shardMd5Hash) {
    // TODO Recovery shard async
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
  public void addContractedPeer(String peerId) throws DistributorException {
    //TODO add new contract
    //TODO send in-balance of 1.
  }

  @Override
  public void run() {
    appraisalLedger.run();
    blockLedger.run();
    contractStore.run();
  }

  @Override
  public void close() {
    appraisalLedger.close();
    blockLedger.close();
    contractStore.close();
  }
}
