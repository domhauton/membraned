package com.domhauton.membrane.distributed.block.ledger;

import com.domhauton.membrane.distributed.block.gen.BlockUtilsTest;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 10/03/17.
 */
class BlockLedgerTest {

  private final static Set<String> BASE_SHARD_SET = Collections.singleton("shard_1");
  private final static String PEER_1 = "peer_1";

  private Path basePath;

  @BeforeEach
  void setUp() throws Exception {
    basePath = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));

  }

  @Test
  void testEmpty() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.confirmBlockHash("foobar", DateTime.now(), "fooSalt"));
  }

  @Test
  void testSingleFillImmediateEnd() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now());
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));

    assertHash(blockLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now().plusHours(1)));
  }

  @Test
  void testSingleFillBeforeEnd() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now().minusDays(1));

    assertHash(blockLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now().plusHours(1)));

  }

  @Test
  void testSingleThreeHourEnd() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now().plusHours(3));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));

    IntStream.range(0, 4).boxed()
        .forEach(x -> assertHash(blockLedger, shard, reference, DateTime.now().plusHours(x)));

    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now().plusHours(4)));
  }

  @Test
  void testIncorrectHashFails() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now().plusHours(3));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));

    DateTime dateTime = DateTime.now().plusHours(3);
    assertHash(blockLedger, shard, reference, dateTime);

    byte[] contractSalt = blockLedger.getBlockEvidenceSalt(reference, dateTime);
    String calculatedHash = blockLedger.getHash(contractSalt, shard);
    Assertions.assertFalse(blockLedger.confirmBlockHash(reference, dateTime, calculatedHash.toUpperCase()));
  }

  private void assertHash(BlockLedger blockLedger, byte[] shard, String reference, DateTime dateTime) {
    byte[] contractSalt = blockLedger.getBlockEvidenceSalt(reference, dateTime);
    String calculatedHash = blockLedger.getHash(contractSalt, shard);
    Assertions.assertTrue(blockLedger.confirmBlockHash(reference, dateTime, calculatedHash));
  }

  @Test
  void testRemoval() throws Exception {
    BlockLedger blockLedger = new BlockLedger(basePath);
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt("foobar", DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.confirmBlockHash("foobar", DateTime.now(), "fooSalt"));

    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now());
    assertHash(blockLedger, shard, reference, DateTime.now());

    blockLedger.removeBlock(reference);

    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.confirmBlockHash(reference, DateTime.now(), "fooSalt"));

    blockLedger.addBlock(shard, BASE_SHARD_SET, PEER_1, DateTime.now());
    assertHash(blockLedger, shard, reference, DateTime.now());

    blockLedger.removeAllExcept(Collections.singleton(reference));
    assertHash(blockLedger, shard, reference, DateTime.now());

    blockLedger.removeAllExcept(Collections.emptySet());

    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.getBlockEvidenceSalt(reference, DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockLedger.confirmBlockHash(reference, DateTime.now(), "fooSalt"));
  }

  @AfterEach
  void tearDown() throws Exception {
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath);
  }
}