package com.domhauton.membrane.distributed;

import com.domhauton.membrane.MockitoExtension;
import com.domhauton.membrane.distributed.block.ledger.BlockLedger;
import com.domhauton.membrane.distributed.contract.ContractStore;
import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import com.domhauton.membrane.distributed.evidence.EvidenceType;
import com.domhauton.membrane.network.NetworkManager;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageImpl;
import com.domhauton.membrane.storage.BackupLedger;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

/**
 * Created by dominic on 19/04/17.
 */
@ExtendWith(MockitoExtension.class)
class DistributorTest {
  private static final long GIGABYTE = 1024 * 1024 * 1024;
  private static final int CONTRACT_LIMIT = 20;
  private static final Random RANDOM = new Random(System.currentTimeMillis());

  private static final String PEER_1 = "peer_1";
  private static final String PEER_2 = "peer_2";
  private static final String SHARD_1 = "shard_1";

  private Distributor distributor1;
  private Path basePath1;
  private BackupLedger backupLedgerMock1;
  private ShardStorage localShardStorage1;
  private ShardStorage peerBlockStorage1;
  private NetworkManager networkManagerMock1;
  private BlockLedger blockLedgerInner1;
  private ContractStore contractStoreInner1;
  private MembraneAuthInfo membraneAuthInfo1;

  private Distributor distributor2;
  private Path basePath2;
  private BackupLedger backupLedgerMock2;
  private ShardStorage localShardStorage2;
  private ShardStorage peerBlockStorage2;
  private NetworkManager networkManagerMock2;
  private BlockLedger blockLedgerInner2;
  private ContractStore contractStoreInner2;
  private MembraneAuthInfo membraneAuthInfo2;

  @Captor
  private ArgumentCaptor<Set<String>> stringSetCaptor;

  @BeforeEach
  void setUp() throws Exception {
    AuthUtils.addProvider();

    basePath1 = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));
    backupLedgerMock1 = Mockito.mock(BackupLedger.class);
    Path localShardStoragePath1 = Paths.get(basePath1.toString() + File.separator + "/localShards");
    Path peerShardStoragePath1 = Paths.get(basePath1.toString() + File.separator + "/peerShards");
    localShardStorage1 = new ShardStorageImpl(localShardStoragePath1, GIGABYTE);
    peerBlockStorage1 = new ShardStorageImpl(peerShardStoragePath1, GIGABYTE, Hashing.sha512());
    networkManagerMock1 = Mockito.mock(NetworkManager.class);
    membraneAuthInfo1 = AuthUtils.generateAuthenticationInfo();
    Mockito.when(networkManagerMock1.getPrivateEncryptionKey()).thenReturn(membraneAuthInfo1.getPrivateKey().getModulus().toString(Character.MAX_RADIX));
    Mockito.when(networkManagerMock1.getUID()).thenReturn(membraneAuthInfo1.getClientId());

    distributor1 = new Distributor(basePath1, backupLedgerMock1, localShardStorage1, peerBlockStorage1, networkManagerMock1, CONTRACT_LIMIT);
    blockLedgerInner1 = extractBlockLedger(distributor1);
    contractStoreInner1 = extractContractStore(distributor1);

    basePath2 = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));
    backupLedgerMock2 = Mockito.mock(BackupLedger.class);
    Path localShardStoragePath2 = Paths.get(basePath2 + File.separator + "/localShards");
    Path peerShardStoragePath2 = Paths.get(basePath2 + File.separator + "/peerShards");
    localShardStorage2 = new ShardStorageImpl(localShardStoragePath2, GIGABYTE);
    peerBlockStorage2 = new ShardStorageImpl(peerShardStoragePath2, GIGABYTE, Hashing.sha512());
    networkManagerMock2 = Mockito.mock(NetworkManager.class);
    membraneAuthInfo2 = AuthUtils.generateAuthenticationInfo();
    Mockito.when(networkManagerMock2.getPrivateEncryptionKey()).thenReturn(membraneAuthInfo2.getPrivateKey().getModulus().toString(Character.MAX_RADIX));
    Mockito.when(networkManagerMock2.getUID()).thenReturn(membraneAuthInfo2.getClientId());

    distributor2 = new Distributor(basePath2, backupLedgerMock2, localShardStorage2, peerBlockStorage2, networkManagerMock2, CONTRACT_LIMIT);
    blockLedgerInner2 = extractBlockLedger(distributor2);
    contractStoreInner2 = extractContractStore(distributor2);
  }

  @Test
  void startStopTest() throws Exception {
    distributor1.run();
    Thread.sleep(1000);
    distributor1.close();
  }

  @Test
  void processFaultyUnexpectedBlockTest() throws Exception {
    setupAllowedInequality();

    String block1Id = StorageManagerTestUtils.addRandBlock(RANDOM, peerBlockStorage1);
    byte[] block1Bytes = peerBlockStorage1.retrieveShard(block1Id);
    blockLedgerInner2.addBlock(block1Bytes, Collections.singleton(SHARD_1), PEER_1, DateTime.now().plusWeeks(1));
    contractStoreInner2.addMyBlockId(PEER_1, block1Id);
    contractStoreInner1.addPeerBlockId(PEER_2, block1Id);

    Assertions.assertTrue(peerBlockStorage1.hasShard(block1Id));

    DateTime baseDateTime = DateTime.now().hourOfDay().roundFloorCopy();

    // Send Update

    distributor1.sendContractUpdateToPeer(PEER_2);
    Mockito.verify(networkManagerMock1, Mockito.times(1)).sendContractUpdateToPeer(PEER_2, baseDateTime, 1, Collections.singleton(block1Id));

    // Ensure requests are as expected

    Set<EvidenceRequest> evidenceRequests = distributor2.processPeerContractUpdate(PEER_1, baseDateTime, 1, Collections.singleton(block1Id));
    Assertions.assertEquals(1, evidenceRequests.size());
    EvidenceRequest request = evidenceRequests.iterator().next();

    Assertions.assertEquals(block1Id, request.getBlockId());
    Assertions.assertEquals(EvidenceType.SEND_BLOCK, request.getEvidenceType());
    Assertions.assertArrayEquals(new byte[0], request.getSalt());

    // Ensure responses are as expected

    Set<EvidenceResponse> evidenceResponses = distributor1.processEvidenceRequests(PEER_2, baseDateTime, evidenceRequests);
    Assertions.assertEquals(1, evidenceResponses.size());
    EvidenceResponse response = evidenceResponses.iterator().next();

    Assertions.assertEquals(block1Id, response.getBlockId());
    Assertions.assertEquals(EvidenceType.SEND_BLOCK, response.getEvidenceType());
    Assertions.assertArrayEquals(block1Bytes, response.getResponse());

    // Ensure responses processed correctly

    distributor2.processEvidenceResponses(PEER_1, baseDateTime, evidenceResponses);
    // Will fail at deconstructing block.

    Assertions.assertTrue(peerBlockStorage1.hasShard(block1Id));
    // Ensure nothing has been put into storage.
    Assertions.assertEquals(0, localShardStorage2.getStorageSize());
    Assertions.assertEquals(0, peerBlockStorage2.getStorageSize());

    Assertions.assertEquals(Collections.singleton(block1Id), contractStoreInner2.getMyBlockIds(PEER_1));
    Assertions.assertEquals(Collections.singleton(block1Id), contractStoreInner1.getPeerBlockIds(PEER_2));
  }

  @Test
  void processFaultyLostBlockTest() throws Exception {
    setupAllowedInequality();

    String block1Id = StorageManagerTestUtils.addRandBlock(RANDOM, peerBlockStorage1);
    byte[] block1Bytes = peerBlockStorage1.retrieveShard(block1Id);
    blockLedgerInner2.addBlock(block1Bytes, Collections.singleton(SHARD_1), PEER_1, DateTime.now().plusWeeks(1));
    contractStoreInner1.addPeerBlockId(PEER_2, block1Id);

    Assertions.assertTrue(peerBlockStorage1.hasShard(block1Id));

    DateTime baseDateTime = DateTime.now().hourOfDay().roundFloorCopy();

    // Send Update

    distributor1.sendContractUpdateToPeer(PEER_2);
    Mockito.verify(networkManagerMock1, Mockito.times(1)).sendContractUpdateToPeer(PEER_2, baseDateTime, 1, Collections.singleton(block1Id));

    // Ensure requests are as expected

    Set<EvidenceRequest> evidenceRequests = distributor2.processPeerContractUpdate(PEER_1, baseDateTime, 1, Collections.singleton(block1Id));
    Assertions.assertEquals(1, evidenceRequests.size());
    EvidenceRequest request = evidenceRequests.iterator().next();

    Assertions.assertEquals(block1Id, request.getBlockId());
    Assertions.assertEquals(EvidenceType.SEND_BLOCK, request.getEvidenceType());
    Assertions.assertArrayEquals(new byte[0], request.getSalt());

    // Ensure responses are as expected

    Set<EvidenceResponse> evidenceResponses = distributor1.processEvidenceRequests(PEER_2, baseDateTime, evidenceRequests);
    Assertions.assertEquals(1, evidenceResponses.size());
    EvidenceResponse response = evidenceResponses.iterator().next();

    Assertions.assertEquals(block1Id, response.getBlockId());
    Assertions.assertEquals(EvidenceType.SEND_BLOCK, response.getEvidenceType());
    Assertions.assertArrayEquals(block1Bytes, response.getResponse());

    // Ensure responses processed correctly

    distributor2.processEvidenceResponses(PEER_1, baseDateTime, evidenceResponses);
    // Will fail at deconstructing block.

    Assertions.assertTrue(peerBlockStorage1.hasShard(block1Id));
    // Ensure nothing has been put into storage.
    Assertions.assertEquals(0, localShardStorage2.getStorageSize());
    Assertions.assertEquals(0, peerBlockStorage2.getStorageSize());

    Assertions.assertEquals(Collections.singleton(block1Id), contractStoreInner2.getMyBlockIds(PEER_1));
    Assertions.assertEquals(Collections.singleton(block1Id), contractStoreInner1.getPeerBlockIds(PEER_2));
  }

  @Test
  void removeLostBlockOnSecondPassTest() throws Exception {
    setupAllowedInequality();

    String block1Id = StorageManagerTestUtils.addRandBlock(RANDOM, peerBlockStorage1);
    contractStoreInner1.addPeerBlockId(PEER_2, block1Id);

    Assertions.assertTrue(peerBlockStorage1.hasShard(block1Id));

    DateTime baseDateTime = DateTime.now().hourOfDay().roundFloorCopy();

    Set<EvidenceRequest> evidenceRequests1 = distributor2.processPeerContractUpdate(PEER_1, baseDateTime, 1, Collections.singleton(block1Id));
    Assertions.assertEquals(1, evidenceRequests1.size());

    Set<EvidenceResponse> evidenceResponses1 = distributor1.processEvidenceRequests(PEER_2, baseDateTime, evidenceRequests1);
    Assertions.assertEquals(1, evidenceResponses1.size());

    Assertions.assertEquals(Collections.singleton(block1Id), contractStoreInner2.getMyBlockIds(PEER_1));
    Assertions.assertEquals(Collections.singleton(block1Id), contractStoreInner1.getPeerBlockIds(PEER_2));

    // NEXT HOUR IT SHOULD BE DELETED!

    DateTime laterDateTime = baseDateTime.plusMinutes(30);

    // Ensure requests are as expected

    Set<EvidenceRequest> evidenceRequests2 = distributor2.processPeerContractUpdate(PEER_1, laterDateTime, 1, Collections.singleton(block1Id));
    Assertions.assertEquals(1, evidenceRequests2.size());
    EvidenceRequest request = evidenceRequests2.iterator().next();

    Assertions.assertEquals(block1Id, request.getBlockId());
    Assertions.assertEquals(EvidenceType.DELETE_BLOCK, request.getEvidenceType());
    Assertions.assertArrayEquals(new byte[0], request.getSalt());

    // Ensure responses are as expected

    Set<EvidenceResponse> evidenceResponses2 = distributor1.processEvidenceRequests(PEER_2, laterDateTime, evidenceRequests2);
    Assertions.assertEquals(0, evidenceResponses2.size());

    // Ensure responses processed correctly

    distributor2.processEvidenceResponses(PEER_1, laterDateTime, evidenceResponses2);

    // Will fail at deconstructing block.

    Assertions.assertFalse(peerBlockStorage1.hasShard(block1Id));
    // Ensure nothing has been put into storage.
    Assertions.assertEquals(0, localShardStorage2.getStorageSize());
    Assertions.assertEquals(0, peerBlockStorage2.getStorageSize());

    Assertions.assertTrue(contractStoreInner2.getMyBlockIds(PEER_1).isEmpty());
    Assertions.assertTrue(contractStoreInner1.getPeerBlockIds(PEER_2).isEmpty());
  }

  @Test
  void uploadToPeerWorksTest() throws Exception {
    setupAllowedInequality();
    setupConnection();

    String block1Id = StorageManagerTestUtils.addRandBlock(RANDOM, localShardStorage2);
    String block2Id = StorageManagerTestUtils.addRandBlock(RANDOM, localShardStorage2);
    Mockito.when(backupLedgerMock2.getAllRequiredShards()).thenReturn(ImmutableSet.of(block1Id, block2Id));

    distributor2.distributeShards();

    ArgumentCaptor<String> peerArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> blockIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<byte[]> blockByteCaptor = ArgumentCaptor.forClass(byte[].class);
    Mockito.verify(networkManagerMock2, Mockito.times(1))
        .uploadBlockToPeer(peerArgumentCaptor.capture(), blockIdArgumentCaptor.capture(), blockByteCaptor.capture());

    Assertions.assertEquals(PEER_1, peerArgumentCaptor.getValue());
    byte[] block1Bytes = blockByteCaptor.getValue();
    String blockId = blockIdArgumentCaptor.getValue();

    Assertions.assertEquals(contractStoreInner2.getMyBlockIds(), Collections.singleton(blockId));
  }

  @Test
  void recoverShardsFromUnexpectedBlockTest() throws Exception {
    setupAllowedInequality();
    setupConnection();

    String shardId1 = StorageManagerTestUtils.addRandShard(RANDOM, localShardStorage2);
    String shardId2 = StorageManagerTestUtils.addRandShard(RANDOM, localShardStorage2);
    Mockito.doAnswer(invocation -> localShardStorage2.listShardIds()).when(backupLedgerMock2).getAllRequiredShards();

    distributor2.distributeShards();

    ArgumentCaptor<String> peerArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> blockIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<byte[]> blockByteCaptor = ArgumentCaptor.forClass(byte[].class);
    Mockito.verify(networkManagerMock2, Mockito.times(1))
        .uploadBlockToPeer(peerArgumentCaptor.capture(), blockIdArgumentCaptor.capture(), blockByteCaptor.capture());

    Assertions.assertEquals(PEER_1, peerArgumentCaptor.getValue());
    byte[] block1Bytes = blockByteCaptor.getValue();
    String blockId = blockIdArgumentCaptor.getValue();

    Assertions.assertEquals(contractStoreInner2.getMyBlockIds(), Collections.singleton(blockId));

    // Completely forget about block
    contractStoreInner2.removeMyBlockId(PEER_1, blockId);
    blockLedgerInner2.removeBlock(blockId);
    localShardStorage2.removeShard(shardId1);
    localShardStorage2.removeShard(shardId2);
    Assertions.assertTrue(contractStoreInner2.getMyBlockIds().isEmpty());

    // Insert block into peer1
    DateTime baseDateTime = DateTime.now().hourOfDay().roundFloorCopy();

    distributor1.receiveBlock(PEER_2, blockId, block1Bytes);

    Mockito.verify(networkManagerMock1, Mockito.times(1))
        .sendContractUpdateToPeer(PEER_2, baseDateTime, 1, Collections.singleton(blockId));
    Assertions.assertEquals(Collections.singleton(blockId), peerBlockStorage1.listShardIds());
    Assertions.assertEquals(Collections.singleton(blockId), contractStoreInner1.getPeerBlockIds(PEER_2));

    // Send to peer2
    Set<EvidenceRequest> evidenceRequests = distributor2.processPeerContractUpdate(PEER_1, baseDateTime, 1, Collections.singleton(blockId));
    Assertions.assertEquals(1, evidenceRequests.size());
    Assertions.assertEquals(EvidenceType.SEND_BLOCK, evidenceRequests.iterator().next().getEvidenceType());
    // Receive requests
    Set<EvidenceResponse> evidenceResponses = distributor1.processEvidenceRequests(PEER_2, baseDateTime, evidenceRequests);
    Assertions.assertEquals(1, evidenceResponses.size());
    Assertions.assertEquals(EvidenceType.SEND_BLOCK, evidenceResponses.iterator().next().getEvidenceType());
    // Receive response
    distributor2.processEvidenceResponses(PEER_1, baseDateTime, evidenceResponses);
    // Shards should be fully recovered now.

    Assertions.assertEquals(ImmutableSet.of(shardId1, shardId2), localShardStorage2.listShardIds());
  }

  private void setupConnection() {
    Mockito.when(networkManagerMock1.peerConnected(PEER_2)).thenReturn(true);
    Mockito.when(networkManagerMock2.peerConnected(PEER_1)).thenReturn(true);
  }

  private void setupAllowedInequality() {
    contractStoreInner2.setPeerAllowedInequality(PEER_1, 1);
    contractStoreInner2.setMyAllowedInequality(PEER_1, 1);
    contractStoreInner1.setPeerAllowedInequality(PEER_2, 1);
    contractStoreInner1.setMyAllowedInequality(PEER_2, 1);
  }

  @AfterEach
  void tearDown() throws Exception {
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath1);
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath2);
  }

  private BlockLedger extractBlockLedger(Distributor distributor) throws Exception {
    Field pexField = distributor.getClass().getDeclaredField("blockLedger");
    pexField.setAccessible(true);
    return (BlockLedger) pexField.get(distributor);
  }

  private ContractStore extractContractStore(Distributor distributor) throws Exception {
    Field pexField = distributor.getClass().getDeclaredField("contractStore");
    pexField.setAccessible(true);
    return (ContractStore) pexField.get(distributor);
  }
}