package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.appraisal.AppraisalLedger;
import com.domhauton.membrane.distributed.contract.ContractStore;
import com.domhauton.membrane.distributed.contract.ContractStoreException;
import com.domhauton.membrane.distributed.evidence.BlockEvidenceLedger;
import com.domhauton.membrane.distributed.maintainance.RateLimiter;
import com.domhauton.membrane.distributed.manifest.DistributedStore;
import com.domhauton.membrane.distributed.manifest.Priority;
import com.domhauton.membrane.network.NetworkManager;
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
  private final static Duration PEER_SEARCH_RATE_LIMIT = Duration.standardSeconds(5);
  private final Logger logger = LogManager.getLogger();

  private final DistributedStore distributedStore;
  private final BlockEvidenceLedger blockEvidenceLedger;
  private final AppraisalLedger appraisalLedger;
  private final RateLimiter uploadRateLimiter;
  private final RateLimiter peerSearchRateLimiter;
  private final ContractStore contractStore;
  private NetworkManager networkManager;

  public Distributor() {
    distributedStore = new DistributedStore();
    blockEvidenceLedger = new BlockEvidenceLedger();
    appraisalLedger = new AppraisalLedger();
    uploadRateLimiter = new RateLimiter(this::beginUpload, UPLOAD_RATE_LIMIT);
    peerSearchRateLimiter = new RateLimiter(this::beginPeerSearch, PEER_SEARCH_RATE_LIMIT);
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
        Iterator<String> availShardsItr = shardsToUploadForPeer.iterator();
        for (int i = 0; i < numberOfShardsToUpload && availShardsItr.hasNext(); i++) {
          uploadShard(peerId, availShardsItr);
        }
        usedShardSet.addAll(shardsToUploadForPeer);
      }
    }

    Set<String> totalUndeployedShards = distributedStore.undeployedShards();
    // If there exist shards with no potential peers schedule peer search
    if (totalUndeployedShards.size() - usedShardSet.size() > 0) {
      peerSearchRateLimiter.schedule();
    }
  }

  void beginPeerSearch() {
    // TODO Check if peer limit reached
    // TODO If reached check there are any bad peers to remove, if not just wait.
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

  private void uploadShard(String peerId, Iterator<String> shardQueue) {
    // TODO Create block.
    // TODO Fill block.
    // TODO Upload block.
    // TODO If successful record all shards in block uploaded in contract.
    // TODO If successful record all shards in block uploaded in distributedStore.
  }

  public void setNetworkManager(NetworkManager networkManager) {
    this.networkManager = networkManager;
  }
}
