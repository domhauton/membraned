package com.domhauton.membrane.distributed;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * Created by dominic on 20/04/17.
 */
class RejectingContractManagerTest {

  private ContractManager contractManager;

  @BeforeEach
  void setUp() {
    contractManager = new RejectingContractManager();
  }

  @Test
  void getContractedPeers() {
    Assertions.assertTrue(contractManager.getContractedPeers().isEmpty());
  }

  @Test
  void getContractCountTarget() {
    Assertions.assertEquals(0, contractManager.getContractCountTarget());
  }

  @Test
  void addContractedPeer() throws Exception {
    Assertions.assertThrows(ContractManagerException.class, () -> contractManager.addContractedPeer("foobar"));
  }

  @Test
  void receiveBlock() throws Exception {
    contractManager.receiveBlock("foobar_peer", "foobar_block", new byte[30]);
  }

  @Test
  void processPeerContractUpdate() throws Exception {
    contractManager.processPeerContractUpdate("foobar_peer", DateTime.now(), 0, Collections.emptySet());
  }

  @Test
  void processEvidenceResponses() throws Exception {
    contractManager.processEvidenceResponses("foobar_peer", DateTime.now(), Collections.emptySet());
  }

  @Test
  void processEvidenceRequests() throws Exception {
    contractManager.processEvidenceRequests("foobar_peer", DateTime.now(), Collections.emptySet());
  }

}