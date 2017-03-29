package com.domhauton.membrane.network.pex;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dominic on 29/03/17.
 */
class PexLedgerTest {

  private PexEntry pexEntry1;
  private PexEntry pexEntry2;
  private PexEntry pexEntry3;
  private PexEntry pexEntry4;
  private PexEntry pexEntry5;

  private final String user1 = "USER_1";
  private final String user2 = "USER_2";
  private final String user3 = "USER_3";
  private final String user4 = "USER_4";

  @BeforeEach
  void setUp() throws Exception {
    pexEntry1 = new PexEntry("192.168.0.1", 80, true, DateTime.now());
    pexEntry2 = new PexEntry("192.168.0.1", 81, true, DateTime.now());
    pexEntry3 = new PexEntry("192.168.0.2", 82, true, DateTime.now());
    pexEntry4 = new PexEntry("192.168.0.2", 83, true, DateTime.now());
    pexEntry5 = new PexEntry("192.168.1.1", 84, true, DateTime.now());
  }

  @Test
  void singleEntryTest() throws Exception {
    PexLedger pexLedger = new PexLedger(3);
    pexLedger.addPexEntry(user1, pexEntry1);
    PexEntry retrievedEntry = pexLedger.getPexEntry(user1).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry1, retrievedEntry);
  }

  @Test
  void overwriteTest() throws Exception {
    PexLedger pexLedger = new PexLedger(3);
    pexLedger.addPexEntry(user1, pexEntry1);
    PexEntry retrievedEntry = pexLedger.getPexEntry(user1).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry1, retrievedEntry);

    pexLedger.addPexEntry(user1, pexEntry2);
    PexEntry retrievedEntry2 = pexLedger.getPexEntry(user1).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry2, retrievedEntry2);
  }

  @Test
  void userOverflowTest() throws Exception {
    PexLedger pexLedger = new PexLedger(3);
    pexLedger.addPexEntry(user1, pexEntry1);
    pexLedger.addPexEntry(user2, pexEntry2);
    pexLedger.addPexEntry(user3, pexEntry3);

    PexEntry retrievedEntry = pexLedger.getPexEntry(user1).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry1, retrievedEntry);

    Assertions.assertIterableEquals(Arrays.asList(user1, user2, user3), pexLedger.availableHosts());

    // overflow users
    pexLedger.addPexEntry(user4, pexEntry4);

    Assertions.assertIterableEquals(Arrays.asList(user2, user3, user4), pexLedger.availableHosts());

    // first entry should have been deleted
    Assertions.assertFalse(pexLedger.getPexEntry(user1).isPresent());

    // other entries should remain
    PexEntry retrievedEntry2 = pexLedger.getPexEntry(user2).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry2, retrievedEntry2);

    PexEntry retrievedEntry3 = pexLedger.getPexEntry(user3).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry3, retrievedEntry3);

    PexEntry retrievedEntry4 = pexLedger.getPexEntry(user4).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry4, retrievedEntry4);

    // Re-add user 1
    pexLedger.addPexEntry(user1, pexEntry5);

    Assertions.assertIterableEquals(Arrays.asList(user3, user4, user1), pexLedger.availableHosts());


    Assertions.assertFalse(pexLedger.getPexEntry(user2).isPresent());

    PexEntry retrievedEntry5 = pexLedger.getPexEntry(user1).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry5, retrievedEntry5);
  }

  @Test
  void serializeTest() throws Exception {
    PexLedger pexLedger = new PexLedger(3);
    pexLedger.addPexEntry(user1, pexEntry1);
    pexLedger.addPexEntry(user2, pexEntry2);
    pexLedger.addPexEntry(user3, pexEntry3);

    String serializedLedger = pexLedger.serialize();
    PexLedger deserializedLedger = PexLedger.deserialize(Arrays.asList(serializedLedger.split("\n")), 3);

    Assertions.assertEquals(pexLedger, deserializedLedger);
  }

  @Test
  void deserializeCorruptedTest() throws Exception {
    PexLedger pexLedger = new PexLedger(3);
    pexLedger.addPexEntry(user1, pexEntry1);
    pexLedger.addPexEntry(user2, pexEntry2);
    pexLedger.addPexEntry(user3, pexEntry3);

    String serializedLedger = pexLedger.serialize();
    List<String> serializedList = new ArrayList<>(Arrays.asList(serializedLedger.split("\n")));
    serializedList.add(0, serializedList.get(0).replaceAll(",", ""));
    serializedList.remove(1);
    PexLedger deserializedLedger = PexLedger.deserialize(serializedList, 3);

    // first entry should have been deleted
    Assertions.assertFalse(deserializedLedger.getPexEntry(user1).isPresent());

    // other entries should remain
    PexEntry retrievedEntry2 = deserializedLedger.getPexEntry(user2).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry2, retrievedEntry2);

    PexEntry retrievedEntry3 = deserializedLedger.getPexEntry(user3).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry3, retrievedEntry3);
  }

  @Test
  void deserializeCorruptedInnerTest() throws Exception {
    PexLedger pexLedger = new PexLedger(3);
    pexLedger.addPexEntry(user1, pexEntry1);
    pexLedger.addPexEntry(user2, pexEntry2);
    pexLedger.addPexEntry(user3, pexEntry3);

    String serializedLedger = pexLedger.serialize();
    List<String> serializedList = new ArrayList<>(Arrays.asList(serializedLedger.split("\n")));
    serializedList.add(0, serializedList.get(0).replaceFirst(",", ""));
    serializedList.remove(1);
    PexLedger deserializedLedger = PexLedger.deserialize(serializedList, 3);

    // first entry should have been deleted
    Assertions.assertFalse(deserializedLedger.getPexEntry(user1).isPresent());

    // other entries should remain
    PexEntry retrievedEntry2 = deserializedLedger.getPexEntry(user2).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry2, retrievedEntry2);

    PexEntry retrievedEntry3 = deserializedLedger.getPexEntry(user3).orElseThrow(Error::new);
    Assertions.assertEquals(pexEntry3, retrievedEntry3);
  }
}