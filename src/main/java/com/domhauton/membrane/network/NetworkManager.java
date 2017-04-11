package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;

import java.io.Closeable;

/**
 * Created by dominic on 06/04/17.
 */
public interface NetworkManager extends Runnable, Closeable {

  /**
   * Set the contract manager in all of the network subsystems
   *
   * @param contractManager ContractManager to set
   */
  void setContractManager(ContractManager contractManager);

  /**
   * Check if peer is connected
   *
   * @param peerId Id of the peer connected
   * @return true if peer is connected.
   */
  boolean peerConnected(String peerId);

  /**
   * Upload block to peer provided. Async with no confirmation of upload.
   *
   * @param peerId              Peer to upload to
   * @param blockData           The bytes inside the block to upload
   * @throws NetworkException   If there was an issue uploading. Peer not connected or buffer full.
   */
  void uploadBlockToPeer(String peerId, byte[] blockData) throws NetworkException;

  /**
   * Sets whether the network manager should allow contracts with new peers.
   *
   * @param shouldSearch If True, allowed.
   */
  void setSearchForNewPublicPeers(boolean shouldSearch);

  /**
   * Returns the UserId of the Network Manager.
   *
   * @return UID generated from the credentials of the Network Manager.
   */
  String getUID();
}
