package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.ContractManagerException;
import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import com.domhauton.membrane.distributed.evidence.EvidenceType;
import com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 11/04/17.
 * <p>
 * For use in tests only. Emulates the behaviour of functional contract manager.
 */
class EvictingContractManager implements ContractManager {
  private final static Logger LOGGER = LogManager.getLogger();

  private final String BLOCK_HASH_OURS;
  private final String BLOCK_DELETE_OURS;
  private final String BLOCK_FULL_OURS;

  final String BLOCK_HASH_PEERS;
  final String BLOCK_DELETE_PEERS;
  final String BLOCK_FULL_PEERS;

  private final byte[] BLOCK_HASH_SALT_OURS;
  private final byte[] BLOCK_HASH_RESP_OURS;
  private final byte[] BLOCK_DATA_FULL_OURS;

  private final byte[] BLOCK_HASH_SALT_PEERS;
  private final byte[] BLOCK_HASH_RESP_PEERS;
  private final byte[] BLOCK_DATA_FULL_PEERS;

  private boolean hashConfirmed = false;
  private boolean fullBlockConfirmed = false;
  private boolean blockDeleted = false;

  private byte[] receivedBlock;
  private String receivedBlockId;

  private EvictingQueue<String> peerQueue;
  private int size;

  EvictingContractManager(int size, String localRand, String remoteRand) {
    this.size = size;
    peerQueue = EvictingQueue.create(size);

    BLOCK_HASH_OURS = "block_hash_" + localRand;
    BLOCK_DELETE_OURS = "block_delete_" + localRand;
    BLOCK_FULL_OURS = "block_full_" + localRand;
    BLOCK_HASH_SALT_OURS = ("block_salt_req" + localRand).getBytes();
    BLOCK_HASH_RESP_OURS = ("block_hash_res" + localRand).getBytes();
    BLOCK_DATA_FULL_OURS = ("block_full" + localRand).getBytes();

    BLOCK_HASH_PEERS = "block_hash_" + remoteRand;
    BLOCK_DELETE_PEERS = "block_delete_" + remoteRand;
    BLOCK_FULL_PEERS = "block_full_" + remoteRand;
    BLOCK_HASH_SALT_PEERS = ("block_salt_req" + remoteRand).getBytes();
    BLOCK_HASH_RESP_PEERS = ("block_hash_res" + remoteRand).getBytes();
    BLOCK_DATA_FULL_PEERS = ("block_full" + remoteRand).getBytes();
  }

  @Override
  public Set<String> getContractedPeers() {
    return new HashSet<>(peerQueue);
  }

  @Override
  public int getContractCountTarget() {
    return size;
  }

  @Override
  public Set<String> getPartiallyDistributedShards() {
    return null; // Do not need
  }

  @Override
  public Set<String> getFullyDistributedShards() {
    return null; // Do not need
  }

  @Override
  public void addContractedPeer(String peerId) throws ContractManagerException {
    synchronized (this) {
      if (!peerQueue.contains(peerId)) {
        peerQueue.add(peerId);
      }
    }
  }

  @Override
  public void receiveBlock(String peerId, String blockId, byte[] data) {
    receivedBlock = data;
    receivedBlockId = blockId;
  }

  @Override
  public Set<EvidenceRequest> processPeerContractUpdate(String peerId, DateTime dateTime, int permittedInequality, Set<String> blockIds) {
    Assertions.assertTrue(permittedInequality > 0);
    return blockIds.stream()
        .map(blockId -> {
          if (blockId.equals(BLOCK_HASH_OURS)) {
            return new EvidenceRequest(blockId, EvidenceType.COMPUTE_HASH, BLOCK_HASH_SALT_OURS);
          } else if (blockId.equals(BLOCK_DELETE_OURS)) {
            return new EvidenceRequest(blockId, EvidenceType.DELETE_BLOCK, BLOCK_HASH_SALT_OURS);
          } else if (blockId.equals(BLOCK_FULL_OURS)) {
            return new EvidenceRequest(blockId, EvidenceType.SEND_BLOCK, BLOCK_HASH_SALT_OURS);
          } else {
            LOGGER.fatal("UNEXPECTED EVIDENCE REQUEST RECEIVED.");
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @Override
  public void processEvidenceResponses(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    for (EvidenceResponse evidenceResponse : evidenceResponses) {
      if (evidenceResponse.getBlockId().equals(BLOCK_HASH_OURS)
          && Arrays.equals(evidenceResponse.getResponse(), BLOCK_HASH_RESP_OURS)
          && evidenceResponse.getEvidenceType() == EvidenceType.COMPUTE_HASH) {
        hashConfirmed = true;
        LOGGER.info("HASH CONFIRMED");
      } else if (evidenceResponse.getBlockId().equals(BLOCK_FULL_OURS)
          && Arrays.equals(evidenceResponse.getResponse(), BLOCK_DATA_FULL_OURS)
          && evidenceResponse.getEvidenceType() == EvidenceType.SEND_BLOCK) {
        fullBlockConfirmed = true;
        LOGGER.info("FULL BLOCK CONFIRMED");
      } else {
        Assertions.fail("UNEXPECTED BLOCK! " + evidenceResponse.getBlockId());
      }
    }
    LOGGER.info("PROCESSED ALL EVIDENCE RESPONSES.");
  }

  @Override
  public Set<EvidenceResponse> processEvidenceRequests(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    return evidenceRequests.stream()
        .map(evidenceRequest -> {
          if (evidenceRequest.getBlockId().equals(BLOCK_HASH_PEERS)
              && evidenceRequest.getEvidenceType() == EvidenceType.COMPUTE_HASH
              && Arrays.equals(evidenceRequest.getSalt(), BLOCK_HASH_SALT_PEERS)) {
            return new EvidenceResponse(evidenceRequest.getBlockId(), evidenceRequest.getEvidenceType(), BLOCK_HASH_RESP_PEERS);
          } else if (evidenceRequest.getBlockId().equals(BLOCK_DELETE_PEERS)
              && evidenceRequest.getEvidenceType() == EvidenceType.DELETE_BLOCK) {
            blockDeleted = true;
            LOGGER.info("BLOCK DELETED");
            return null;
          } else if (evidenceRequest.getBlockId().equals(BLOCK_FULL_PEERS)
              && evidenceRequest.getEvidenceType() == EvidenceType.SEND_BLOCK) {
            return new EvidenceResponse(evidenceRequest.getBlockId(), evidenceRequest.getEvidenceType(), BLOCK_DATA_FULL_PEERS);
          } else {
            LOGGER.fatal("ILLEGAL BLOCK EVIDENCE REQUEST RECEIVED.");
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @Override
  public void close() {
    // Do Nothing
  }

  boolean isHashConfirmed() {
    return hashConfirmed;
  }

  boolean isFullBlockConfirmed() {
    return fullBlockConfirmed;
  }

  boolean isBlockDeleted() {
    return blockDeleted;
  }

  public byte[] getReceivedBlock() {
    return receivedBlock;
  }

  public String getReceivedBlockId() {
    return receivedBlockId;
  }

  void clearBlock() {
    receivedBlockId = null;
    receivedBlock = null;
  }

  @Override
  public void run() {
    // Do Nothing
  }
}
