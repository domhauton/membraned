package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.appraisal.AppraisalLedger;
import com.domhauton.membrane.distributed.block.BlockException;
import com.domhauton.membrane.distributed.block.BlockProcessor;
import com.domhauton.membrane.distributed.contract.ContractStore;
import com.domhauton.membrane.distributed.contract.ContractStoreException;
import com.domhauton.membrane.distributed.evidence.BlockEvidenceLedger;
import com.domhauton.membrane.distributed.maintainance.RateLimiter;
import com.domhauton.membrane.distributed.manifest.DistributedStore;
import com.domhauton.membrane.distributed.manifest.Priority;
import com.domhauton.membrane.network.NetworkException;
import com.domhauton.membrane.network.NetworkManager;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class Distributor implements Runnable {
  private final static Duration UPLOAD_RATE_LIMIT = Duration.standardSeconds(5);
  private final static long BLOCK_SIZE_BYTES = 16L * 1024L * 1024L * 1024L; //16MB
  private final Logger logger = LogManager.getLogger();

  private final ShardStorage shardStorage;

  private final DistributedStore distributedStore;
  private final BlockEvidenceLedger blockEvidenceLedger;
  private final AppraisalLedger appraisalLedger;
  private final RateLimiter uploadRateLimiter;
  private final ContractStore contractStore;
  private NetworkManager networkManager;

  public Distributor(ShardStorage shardStorage) {
    this.shardStorage = shardStorage;

    distributedStore = new DistributedStore();
    blockEvidenceLedger = new BlockEvidenceLedger();
    appraisalLedger = new AppraisalLedger();
    uploadRateLimiter = new RateLimiter(this::beginUpload, UPLOAD_RATE_LIMIT);
    contractStore = new ContractStore();
  }

  public void storeShard(String md5Hash, Priority priority) {
    distributedStore.addDistributedShard(md5Hash, priority);
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
      Set<String> shardsToUploadForPeer = distributedStore.undeployedShards(peerId);
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
          // TODO Record all shards in block uploaded in contract.
          // TODO Record all shards in block uploaded in distributedStore.
          if (uploadedShards.isEmpty()) { // If upload was useless, there is no more to upload.
            shardsToUploadForPeer.clear();
          } else {
            shardsToUploadForPeer.removeAll(uploadedShards);
          }
        }
        usedShardSet.addAll(shardsToUploadForPeer);
      }
    }

    Set<String> totalUndeployedShards = distributedStore.undeployedShards();
    // If there exist shards with no potential peers schedule peer search
    if (totalUndeployedShards.size() - usedShardSet.size() > 0) {
      // Schedule new scan
    }
  }

  void updateContractedPeers(Set<String> peerIds) {
    if (networkManager != null) {
      networkManager.setContractedPeerList(peerIds);
    }
  }

  void updatePeerBlockList(Set<String> peerIds) {
    if (networkManager != null) {
      networkManager.setPeerBlockList(peerIds);
    }
  }

  public void run() {
    appraisalLedger.run();
  }

  public void close() {
    appraisalLedger.close();
  }

  private boolean isPeerConnected(String peerId) {
    return networkManager != null && networkManager.peerConnected(peerId);
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

    try {
      uploadBlock(peerId, blockProcessor);
      return uploadedSet;
    } catch (DistributorException e) {
      logger.warn("Unable to upload generated block: {}", e.getMessage());
      return Collections.emptySet();
    }
  }

  private void uploadBlock(String peerId, BlockProcessor blockProcessor) throws DistributorException {
    // Send created block to peer.
    try {
      byte[] blockData = blockProcessor.toBytes();

      // FIXME encrypt

      if (networkManager != null) {
        networkManager.uploadBlockToPeer(peerId, blockData);
      } else {
        throw new DistributorException("Not connected to network! Can't upload.");
      }
    } catch (BlockException | NetworkException e) {
      throw new DistributorException("Unable to upload generated block", e);
    }
  }

  public void requestShardRecovery(String shardMd5Hash) {
    // TODO Recovery shard async
  }

  public void setNetworkManager(NetworkManager networkManager) {
    this.networkManager = networkManager;
  }
}
