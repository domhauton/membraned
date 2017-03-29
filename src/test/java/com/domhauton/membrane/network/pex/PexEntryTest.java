package com.domhauton.membrane.network.pex;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by dominic on 29/03/17.
 */
class PexEntryTest {

  @Test
  void testSerialisation() throws Exception {
    PexEntry entry1 = new PexEntry("192.168.0.1", 80, true, DateTime.now());
    String serialized = entry1.serialize();
    PexEntry deserializedEntry = PexEntry.deserialize(serialized);
    Assertions.assertEquals(entry1, deserializedEntry);

    PexEntry entry2 = new PexEntry("192.168.0.1", 80, false, DateTime.now());
    String serialized2 = entry2.serialize();
    PexEntry deserializedEntry2 = PexEntry.deserialize(serialized2);
    Assertions.assertEquals(entry2, deserializedEntry2);
  }

  @Test
  void failOnOutOfBoundsPort() throws Exception {
    Assertions.assertThrows(PexException.class, () -> new PexEntry("192.168.0.1", -1, true, DateTime.now()));
    Assertions.assertThrows(PexException.class, () -> new PexEntry("192.168.0.1", 65536, true, DateTime.now()));
  }

  @Test
  void corruptEntryTest() throws Exception {
    DateTime now = DateTime.now();
    PexEntry entry1 = new PexEntry("192.168.0.1", 80, true, now);
    String serialized = entry1.serialize();

    // Ensure valid
    PexEntry deserializedEntry = PexEntry.deserialize(serialized);
    Assertions.assertEquals(entry1, deserializedEntry);

    // Now corrupt it
    String serializedCorruptUnder = serialized.replace("80", "-1");
    Assertions.assertThrows(PexException.class, () -> PexEntry.deserialize(serializedCorruptUnder));

    String serializedCorruptOver = serialized.replace("80", "65536");
    Assertions.assertThrows(PexException.class, () -> PexEntry.deserialize(serializedCorruptOver));

    String serializedCorruptPortInvalid = serialized.replace("80", "foo");
    Assertions.assertThrows(PexException.class, () -> PexEntry.deserialize(serializedCorruptPortInvalid));

    String serializedCorruptSepRemoved = serialized.replace(",", "");
    Assertions.assertThrows(PexException.class, () -> PexEntry.deserialize(serializedCorruptSepRemoved));

    String corruptTimeRemoved = serialized.replace(Long.toString(now.getMillis()), "failtime");
    Assertions.assertThrows(PexException.class, () -> PexEntry.deserialize(corruptTimeRemoved));

    String corruptTimeInvalid = serialized.replace(Long.toString(now.getMillis()), "-1");
    Assertions.assertThrows(PexException.class, () -> PexEntry.deserialize(corruptTimeInvalid));

    String corruptPrivacy = serialized.replace("PUB", "FAKE");
    Assertions.assertThrows(PexException.class, () -> PexEntry.deserialize(corruptPrivacy));
  }
}