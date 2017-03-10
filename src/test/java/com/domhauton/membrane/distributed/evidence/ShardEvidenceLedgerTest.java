package com.domhauton.membrane.distributed.evidence;

import com.domhauton.membrane.distributed.shard.ShardDataUtilsTest;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 10/03/17.
 */
class ShardEvidenceLedgerTest {

  @Test
  void testEmpty() throws Exception {
    ShardEvidenceLedger shardEvidenceLedger = new ShardEvidenceLedger();
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt("foobar", DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.confirmContractSaltHash("foobar", DateTime.now(), "fooSalt"));
  }

  @Test
  void testSingleFillImmediateEnd() throws Exception {
    ShardEvidenceLedger shardEvidenceLedger = new ShardEvidenceLedger();
    byte[] shard = ShardDataUtilsTest.generateRandomShard();
    String reference = shardEvidenceLedger.addNewContract(shard, DateTime.now());
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt("foobar", DateTime.now()));

    assertHash(shardEvidenceLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt(reference, DateTime.now().plusHours(1)));
  }

  @Test
  void testSingleFillBeforeEnd() throws Exception {
    ShardEvidenceLedger shardEvidenceLedger = new ShardEvidenceLedger();
    byte[] shard = ShardDataUtilsTest.generateRandomShard();
    String reference = shardEvidenceLedger.addNewContract(shard, DateTime.now().minusDays(1));

    assertHash(shardEvidenceLedger, shard, reference, DateTime.now());

    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt(reference, DateTime.now().plusHours(1)));

  }

  @Test
  void testSingleThreeHourEnd() throws Exception {
    ShardEvidenceLedger shardEvidenceLedger = new ShardEvidenceLedger();
    byte[] shard = ShardDataUtilsTest.generateRandomShard();
    String reference = shardEvidenceLedger.addNewContract(shard, DateTime.now().plusHours(3));
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt("foobar", DateTime.now()));

    IntStream.range(0, 4).boxed()
            .forEach(x -> assertHash(shardEvidenceLedger, shard, reference, DateTime.now().plusHours(x)));

    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt(reference, DateTime.now().plusHours(4)));
  }

  @Test
  void testIncorrectHashFails() throws Exception {
    ShardEvidenceLedger shardEvidenceLedger = new ShardEvidenceLedger();
    byte[] shard = ShardDataUtilsTest.generateRandomShard();
    String reference = shardEvidenceLedger.addNewContract(shard, DateTime.now().plusHours(3));
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt("foobar", DateTime.now()));

    DateTime dateTime = DateTime.now().plusHours(3);
    assertHash(shardEvidenceLedger, shard, reference, dateTime);

    byte[] contractSalt = shardEvidenceLedger.getContractSalt(reference, dateTime);
    String calculatedHash = ShardEvidenceUtils.getHash(contractSalt, shard);
    Assertions.assertFalse(shardEvidenceLedger.confirmContractSaltHash(reference, dateTime, calculatedHash.toUpperCase()));
  }

  private void assertHash(ShardEvidenceLedger shardEvidenceLedger, byte[] shard, String reference, DateTime dateTime) {
    byte[] contractSalt = shardEvidenceLedger.getContractSalt(reference, dateTime);
    String calculatedHash = ShardEvidenceUtils.getHash(contractSalt, shard);
    Assertions.assertTrue(shardEvidenceLedger.confirmContractSaltHash(reference, dateTime, calculatedHash));
  }

  @Test
  void testRemoval() throws Exception {
    ShardEvidenceLedger shardEvidenceLedger = new ShardEvidenceLedger();
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt("foobar", DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.confirmContractSaltHash("foobar", DateTime.now(), "fooSalt"));

    byte[] shard = ShardDataUtilsTest.generateRandomShard();
    String reference = shardEvidenceLedger.addNewContract(shard, DateTime.now());
    assertHash(shardEvidenceLedger, shard, reference, DateTime.now());

    shardEvidenceLedger.removeContract(reference);

    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt(reference, DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.confirmContractSaltHash(reference, DateTime.now(), "fooSalt"));

    shardEvidenceLedger.addNewContract(shard, DateTime.now());
    assertHash(shardEvidenceLedger, shard, reference, DateTime.now());

    shardEvidenceLedger.removeAllExcept(Collections.singleton(reference));
    assertHash(shardEvidenceLedger, shard, reference, DateTime.now());

    shardEvidenceLedger.removeAllExcept(Collections.emptySet());

    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.getContractSalt(reference, DateTime.now()));
    Assertions.assertThrows(NoSuchElementException.class, () -> shardEvidenceLedger.confirmContractSaltHash(reference, DateTime.now(), "fooSalt"));
  }
}