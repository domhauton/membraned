package com.domhauton.membrane.distributed.block.ledger;

import com.domhauton.membrane.distributed.block.gen.BlockUtilsTest;
import com.domhauton.membrane.distributed.block.manifest.ShardPeerLookup;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 10/03/17.
 */
class BlockLedgerTest {

  private final static String PEER_1 = "peer_1";
  private final static String PEER_2 = "peer_2";
  private final static String SHARD_1 = "shard_1";
  private final static String SHARD_2 = "shard_2";
  private final static String SHARD_3 = "shard_3";
  private final static Set<String> BASE_SHARD_SET = Collections.singleton(SHARD_1);

  private Path basePath;

  @BeforeEach
  void setUp() throws Exception {
    basePath = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));

  }

  @Test
  void testEmpty() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.confirmBlockHash("foobar", DateTime.now(), "fooSalt"));
  }

  @Test
  void testSingleFillImmediateEnd() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now());
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));

    assertHash(blockLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now().plusHours(1)));
  }

  @Test
  void persistenceSimpleTest() throws Exception {
    basePath = Paths.get(basePath.toString() + File.separator + "test_dir");
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] block1 = BlockUtilsTest.generateRandomShard();
    Set<String> blockShards1 = new HashSet<>(Arrays.asList(SHARD_1, SHARD_2));
    byte[] block2 = BlockUtilsTest.generateRandomShard();
    Set<String> blockShards2 = Collections.singleton(SHARD_3);

    String reference1 = blockLedger.addBlock(block1, blockShards1, PEER_1, DateTime.now());
    String reference2 = blockLedger.addBlock(block2, blockShards2, PEER_2, DateTime.now());

    blockLedger.writeBlockInfo();
    blockLedger = null; // Nullify for NPE on use

    BlockLedger blockLedger2 = new BlockLedger(basePath);

    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger2.getBlockEvidenceSalt("foobar", DateTime.now()));

    assertHash(blockLedger2, block1, reference1, DateTime.now());
    assertHash(blockLedger2, block2, reference2, DateTime.now());

    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger2.getBlockEvidenceSalt(reference1, DateTime.now().plusHours(1)));

    ShardPeerLookup shardPeerLookup = blockLedger2.generateShardPeerLookup();
    Assertions.assertEquals(blockShards2, shardPeerLookup.getShardsRequiringPeers(PEER_1));
    Assertions.assertEquals(blockShards1, shardPeerLookup.getShardsRequiringPeers(PEER_2));
  }

  @Test
  void persistenceRemoveBlockTest() throws Exception {
    basePath = Paths.get(basePath.toString() + File.separator + "test_dir");
    BlockLedger blockLedger = new BlockLedger(basePath);
    blockLedger.run();
    byte[] block1 = BlockUtilsTest.generateRandomShard();
    Set<String> blockShards1 = new HashSet<>(Arrays.asList(SHARD_1, SHARD_2));
    byte[] block2 = BlockUtilsTest.generateRandomShard();
    Set<String> blockShards2 = Collections.singleton(SHARD_3);

    String reference1 = blockLedger.addBlock(block1, blockShards1, PEER_1, DateTime.now());
    String reference2 = blockLedger.addBlock(block2, blockShards2, PEER_2, DateTime.now());
    blockLedger.close();
    blockLedger = null; // Nullify for NPE on use

    BlockLedger blockLedger2 = new BlockLedger(basePath);
    blockLedger2.run();
    blockLedger2.removeBlock(reference2);
    blockLedger2.close();
    blockLedger2 = null; // Nullify for NPE on use

    BlockLedger blockLedger3 = new BlockLedger(basePath);
    blockLedger3.run();
    blockLedger3.removeBlock(reference2);


    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger3.getBlockEvidenceSalt("foobar", DateTime.now()));

    assertHash(blockLedger3, block1, reference1, DateTime.now());
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger3.getBlockEvidenceSalt(reference2, DateTime.now()));
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger3.getBlockEvidenceSalt(reference1, DateTime.now().plusHours(1)));

    blockLedger3.addBlock(block2, blockShards2, PEER_1, DateTime.now());
    Set<String> allShards = new HashSet<>(Arrays.asList(SHARD_1, SHARD_2, SHARD_3));
    ShardPeerLookup shardPeerLookup = blockLedger3.generateShardPeerLookup();
    Assertions.assertEquals(Collections.emptySet(), shardPeerLookup.getShardsRequiringPeers(PEER_1));
    Assertions.assertEquals(allShards, shardPeerLookup.getShardsRequiringPeers(PEER_2));
    blockLedger3.close();
  }

  @Test
  void testSingleFillBeforeEnd() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now().minusDays(1));

    assertHash(blockLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now().plusHours(1)));
  }

  @Test
  void testSingleThreeHourEnd() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now().plusHours(3));
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));

    IntStream.range(0, 4).boxed()
        .forEach(x -> {
          try {
            assertHash(blockLedger, shard, reference, DateTime.now().plusHours(x));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now().plusHours(4)));
  }

  @Test
  void testIncorrectHashFails() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now().plusHours(3));
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));

    DateTime dateTime = DateTime.now().plusHours(3);
    assertHash(blockLedger, shard, reference, dateTime);

    byte[] contractSalt = blockLedger.getBlockEvidenceSalt(reference, dateTime);
    String calculatedHash = BlockLedger.getHMAC(contractSalt, shard);
    Assertions.assertFalse(blockLedger.confirmBlockHash(reference, dateTime, calculatedHash.toUpperCase()));
  }

  private void assertHash(BlockLedger blockLedger, byte[] shard, String reference, DateTime dateTime) throws Exception {
    byte[] contractSalt = blockLedger.getBlockEvidenceSalt(reference, dateTime);
    String stringSalt = new String(contractSalt);
    String calculatedHash = stringSalt.equals(BlockLedger.PROOF_TYPE.FULL.toString()) || stringSalt.equals(BlockLedger.PROOF_TYPE.EMPTY.toString()) ? "" : BlockLedger.getHMAC(contractSalt, shard);

    Assertions.assertTrue(blockLedger.confirmBlockHash(reference, dateTime, calculatedHash));
  }

  @Test
  void testRemoval() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.confirmBlockHash("foobar", DateTime.now(), "fooSalt"));

    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now());
    assertHash(blockLedger, shard, reference, DateTime.now());

    blockLedger.removeBlock(reference);

    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now()));
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.confirmBlockHash(reference, DateTime.now(), "fooSalt"));

    blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now());
    assertHash(blockLedger, shard, reference, DateTime.now());

    blockLedger.removeAllExcept(Collections.singleton(reference));
    assertHash(blockLedger, shard, reference, DateTime.now());

    blockLedger.removeAllExcept(Collections.emptySet());

    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now()));
    Assertions.assertThrows(BlockLedgerException.class, () -> blockLedger.confirmBlockHash(reference, DateTime.now(), "fooSalt"));
  }

  @AfterEach
  void tearDown() throws Exception {
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath);
  }
}