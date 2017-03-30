package com.domhauton.membrane.distributed.contract;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 12/03/17.
 */
class ContractStoreTest {

  private final String peer1 = "peer1";
  private final String peer2 = "peer2";

  @Test
  void addMyBlockTest() throws Exception {
    ContractStore contractStore = new ContractStore();
    contractStore.setMyAllowedInequality(peer1, 1);
    contractStore.setPeerAllowedInequality(peer1, 1);
    Assertions.assertEquals(1, contractStore.getMyBlockSpace(peer1));
    contractStore.addMyBlockId(peer1, "block1");
    Assertions.assertEquals(0, contractStore.getMyBlockSpace(peer1));
    contractStore.addMyBlockId(peer1, "block1");
    Assertions.assertThrows(ContractStoreException.class, () -> contractStore.addMyBlockId(peer1, "block2"));
  }

  @Test
  void addPeerBlockTest() throws Exception {
    ContractStore contractStore = new ContractStore();
    contractStore.setMyAllowedInequality(peer1, 1);
    contractStore.setPeerAllowedInequality(peer1, 1);
    Assertions.assertEquals(1, contractStore.getMyBlockSpace(peer1));
    contractStore.addPeerBlockId(peer1, "block1");
    Assertions.assertEquals(2, contractStore.getMyBlockSpace(peer1));
    contractStore.addPeerBlockId(peer1, "block1");
    Assertions.assertThrows(ContractStoreException.class, () -> contractStore.addPeerBlockId(peer1, "block2"));
  }

  @Test
  void peerInequalityTest() throws Exception {
    ContractStore contractStore = new ContractStore();
    Assertions.assertEquals(0, contractStore.getMyBlockSpace(peer1));
    contractStore.setMyAllowedInequality(peer1, 1);
    Assertions.assertEquals(1, contractStore.getMyBlockSpace(peer1));
    Assertions.assertThrows(ContractStoreException.class, () -> contractStore.addPeerBlockId(peer1, "block1"));
    contractStore.setPeerAllowedInequality(peer1, 1);
    contractStore.addPeerBlockId(peer1, "block1");
    Assertions.assertEquals(2, contractStore.getMyBlockSpace(peer1));
    contractStore.addPeerBlockId(peer1, "block1");
    Assertions.assertThrows(ContractStoreException.class, () -> contractStore.addPeerBlockId(peer1, "block2"));
  }

  @Test
  void addBothBlockTest() throws Exception {
    ContractStore contractStore = new ContractStore();
    contractStore.setMyAllowedInequality(peer1, 1);
    contractStore.setPeerAllowedInequality(peer1, 1);
    Assertions.assertEquals(1, contractStore.getMyBlockSpace(peer1));
    contractStore.addPeerBlockId(peer1, "block1");
    Assertions.assertEquals(2, contractStore.getMyBlockSpace(peer1));
    contractStore.addPeerBlockId(peer1, "block1");
    Assertions.assertThrows(ContractStoreException.class, () -> contractStore.addPeerBlockId(peer1, "block2"));
    contractStore.addMyBlockId(peer1, "block1");
    Assertions.assertEquals(1, contractStore.getMyBlockSpace(peer1));
    contractStore.addMyBlockId(peer1, "block2");
    Assertions.assertEquals(0, contractStore.getMyBlockSpace(peer1));
  }

  @Test
  void scalingTest() throws Exception {
    ContractStore contractStore = new ContractStore();
    contractStore.setMyAllowedInequality(peer1, 1);
    contractStore.setPeerAllowedInequality(peer1, 1);
    for (int i = 0; i < 10; i++) {
      contractStore.addPeerBlockId(peer1, "block" + i);
      contractStore.addMyBlockId(peer1, "block" + i);
    }
    Assertions.assertEquals(2, contractStore.getMyBlockSpace(peer1));
    for (int i = 10; i < 20; i++) {
      contractStore.addPeerBlockId(peer1, "block" + i);
      contractStore.addMyBlockId(peer1, "block" + i);
    }
    Assertions.assertEquals(3, contractStore.getMyBlockSpace(peer1));
    for (int i = 0; i < 10; i++) {
      contractStore.removePeerBlockId(peer1, "block" + i);
    }
    Assertions.assertEquals(0, contractStore.getMyBlockSpace(peer1));
    for (int i = 10; i < 20; i++) {
      contractStore.removePeerBlockId(peer1, "block" + i);
    }
    Assertions.assertEquals(0, contractStore.getMyBlockSpace(peer1));
    for (int i = 0; i < 10; i++) {
      contractStore.removeMyBlockId(peer1, "block" + i);
    }
    Assertions.assertEquals(0, contractStore.getMyBlockSpace(peer1));
    for (int i = 10; i < 20; i++) {
      contractStore.removeMyBlockId(peer1, "block" + i);
    }
    Assertions.assertEquals(1, contractStore.getMyBlockSpace(peer1));
  }

  @Test
  void blockListingTest() throws Exception {
    ContractStore contractStore = new ContractStore();
    contractStore.setMyAllowedInequality(peer1, 1);
    contractStore.setPeerAllowedInequality(peer1, 1);
    contractStore.setMyAllowedInequality(peer2, 1);
    contractStore.setPeerAllowedInequality(peer2, 1);
    Set<String> blockSet1 = IntStream.range(0, 10).boxed().map(x -> "block" + x).collect(Collectors.toSet());
    Set<String> blockSet2 = IntStream.range(10, 20).boxed().map(x -> "block" + x).collect(Collectors.toSet());
    blockSet1.forEach(x -> addBlockEvenly(contractStore, x, peer1));
    blockSet2.forEach(x -> addBlockEvenly(contractStore, x, peer2));


    Set<String> peer1BlockIds = contractStore.getPeerBlockIds(peer1);
    Set<String> peer2BlockIds = contractStore.getPeerBlockIds(peer2);
    Assertions.assertEquals(blockSet1, peer1BlockIds);
    Assertions.assertEquals(blockSet2, peer2BlockIds);


    Set<String> myBlockIds = new HashSet<>(contractStore.getMyBlockIds(peer1));
    myBlockIds.addAll(contractStore.getMyBlockIds(peer2));
    blockSet1.addAll(blockSet2);
    Assertions.assertEquals(myBlockIds, blockSet1);
  }

  private void addBlockEvenly(ContractStore contractStore, String block, String peer) {
    try {
      contractStore.addMyBlockId(peer, block);
      contractStore.addPeerBlockId(peer, block);
    } catch (ContractStoreException e) {
      throw new Error(e);
    }
  }
}