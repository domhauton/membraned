package com.domhauton.membrane.distributed.appraisal;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * Created by Dominic Hauton on 05/03/17.
 */
class UptimeCalculatorTest {

  @Test
  void spamUpdateUptimeTest() throws Exception {
    UptimeCalculator uptimeCalculator = new UptimeCalculator();
    for (int i = 0; i < 3; i++) {
      Thread.sleep(50);
      uptimeCalculator.updateUptime();
    }
    double[] uptimeDistribution = uptimeCalculator.getUptimeDistribution();

    int zeroValues = 0;
    int nonZeroValues = 0;
    for (double val : uptimeDistribution) {
      if (val != 0) {
        nonZeroValues++;
        Assertions.assertTrue(val < ((double) 200 / DateTimeConstants.MILLIS_PER_HOUR));
        Assertions.assertTrue(val > ((double) 100 / DateTimeConstants.MILLIS_PER_HOUR));
      } else {
        zeroValues++;
      }
    }

    // Very unlikely but possible to fail if test is run exactly on the hour.

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - 1, zeroValues);
    Assertions.assertEquals(1, nonZeroValues);
  }

  @Test
  void fillWeekTest() throws Exception {
    UptimeCalculator uptimeCalculator = new UptimeCalculator();
    DateTime dateTime = DateTime.now().plusWeeks(1).plusMillis(1500);

    uptimeCalculator.updateUptime(dateTime);
    double[] uptimeDistribution = uptimeCalculator.getUptimeDistribution(dateTime);

    System.out.println(Arrays.toString(uptimeDistribution));

    int oneValues = 0;
    int nonOneValues = 0;
    for (double val : uptimeDistribution) {
      if (val > 1) {
        Assertions.fail("No value should be above one.");
      } else if (val == 1) {
        oneValues++;
      } else {
        nonOneValues++;
        Assertions.assertTrue(val < ((double) 2250 / DateTimeConstants.MILLIS_PER_HOUR) + 0.5);
        Assertions.assertTrue(val > ((double) 750 / DateTimeConstants.MILLIS_PER_HOUR) + 0.5);
      }
    }

    // Very unlikely but possible to fail if test is run exactly on the hour.

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - 1, oneValues);
    Assertions.assertEquals(1, nonOneValues);
  }
}