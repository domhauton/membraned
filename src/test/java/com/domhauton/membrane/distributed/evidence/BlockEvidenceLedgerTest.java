package com.domhauton.membrane.distributed.evidence;

import com.domhauton.membrane.distributed.block.BlockUtilsTest;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 10/03/17.
 */
class BlockEvidenceLedgerTest {

  @Test
  void testEmpty() throws Exception {
    BlockEvidenceLedger blockEvidenceLedger = new BlockEvidenceLedger();
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt("foobar", DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.confirmContractSaltHash("foobar", DateTime.now(), "fooSalt"));
  }

  @Test
  void testSingleFillImmediateEnd() throws Exception {
    BlockEvidenceLedger blockEvidenceLedger = new BlockEvidenceLedger();
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockEvidenceLedger.addNewContract(shard, DateTime.now());
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt("foobar", DateTime.now()));

    assertHash(blockEvidenceLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt(reference, DateTime.now().plusHours(1)));
  }

  @Test
  void testSingleFillBeforeEnd() throws Exception {
    BlockEvidenceLedger blockEvidenceLedger = new BlockEvidenceLedger();
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockEvidenceLedger.addNewContract(shard, DateTime.now().minusDays(1));

    assertHash(blockEvidenceLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt(reference, DateTime.now().plusHours(1)));

  }

  @Test
  void testSingleThreeHourEnd() throws Exception {
    BlockEvidenceLedger blockEvidenceLedger = new BlockEvidenceLedger();
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockEvidenceLedger.addNewContract(shard, DateTime.now().plusHours(3));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt("foobar", DateTime.now()));

    IntStream.range(0, 4).boxed()
            .forEach(x -> assertHash(blockEvidenceLedger, shard, reference, DateTime.now().plusHours(x)));

    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt(reference, DateTime.now().plusHours(4)));
  }

  @Test
  void testIncorrectHashFails() throws Exception {
    BlockEvidenceLedger blockEvidenceLedger = new BlockEvidenceLedger();
    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockEvidenceLedger.addNewContract(shard, DateTime.now().plusHours(3));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt("foobar", DateTime.now()));

    DateTime dateTime = DateTime.now().plusHours(3);
    assertHash(blockEvidenceLedger, shard, reference, dateTime);

    byte[] contractSalt = blockEvidenceLedger.getContractSalt(reference, dateTime);
    String calculatedHash = BlockEvidenceUtils.getHash(contractSalt, shard);
    Assertions.assertFalse(blockEvidenceLedger.confirmContractSaltHash(reference, dateTime, calculatedHash.toUpperCase()));
  }

  private void assertHash(BlockEvidenceLedger blockEvidenceLedger, byte[] shard, String reference, DateTime dateTime) {
    byte[] contractSalt = blockEvidenceLedger.getContractSalt(reference, dateTime);
    String calculatedHash = BlockEvidenceUtils.getHash(contractSalt, shard);
    Assertions.assertTrue(blockEvidenceLedger.confirmContractSaltHash(reference, dateTime, calculatedHash));
  }

  @Test
  void testRemoval() throws Exception {
    BlockEvidenceLedger blockEvidenceLedger = new BlockEvidenceLedger();
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt("foobar", DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.confirmContractSaltHash("foobar", DateTime.now(), "fooSalt"));

    byte[] shard = BlockUtilsTest.generateRandomShard();
    String reference = blockEvidenceLedger.addNewContract(shard, DateTime.now());
    assertHash(blockEvidenceLedger, shard, reference, DateTime.now());

    blockEvidenceLedger.removeContract(reference);

    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt(reference, DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.confirmContractSaltHash(reference, DateTime.now(), "fooSalt"));

    blockEvidenceLedger.addNewContract(shard, DateTime.now());
    assertHash(blockEvidenceLedger, shard, reference, DateTime.now());

    blockEvidenceLedger.removeAllExcept(Collections.singleton(reference));
    assertHash(blockEvidenceLedger, shard, reference, DateTime.now());

    blockEvidenceLedger.removeAllExcept(Collections.emptySet());

    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.getContractSalt(reference, DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> blockEvidenceLedger.confirmContractSaltHash(reference, DateTime.now(), "fooSalt"));
  }
}