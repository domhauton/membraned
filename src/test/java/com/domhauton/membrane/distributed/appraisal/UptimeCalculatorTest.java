package com.domhauton.membrane.distributed.appraisal;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertEquals(0.85, val, 0.16);
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
    DateTime baseDateTime = DateTime.now();
    UptimeCalculator uptimeCalculator = new UptimeCalculator(baseDateTime, baseDateTime);
    DateTime dateTime = baseDateTime.plusWeeks(1).plusMillis(100);

    uptimeCalculator.updateUptime(dateTime);
    double[] uptimeDistribution = uptimeCalculator.getUptimeDistribution(dateTime);

    int oneValues = 0;
    for (double val : uptimeDistribution) {
      if (val == 1) {
        oneValues++;
      } else {
        Assertions.fail("No value should be above one.");
      }
    }

    // Very unlikely but possible to fail if test is run exactly on the hour.

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK, oneValues);
  }

  @Test
  void fillEightDaysTest() throws Exception {
    DateTime baseDateTime = DateTime.now(DateTimeZone.UTC);
    UptimeCalculator uptimeCalculator = new UptimeCalculator(baseDateTime, baseDateTime);
    DateTime dateTime = baseDateTime.plusWeeks(1).plusMillis(100);

    uptimeCalculator.updateUptime(dateTime);
    double[] uptimeDistribution = uptimeCalculator.getUptimeDistribution(dateTime.plusDays(1));

    int oneValuesCnt = 0;
    int halfValuesCnt = 0;
    double nonOneValuesSum = 0;
    for (double val : uptimeDistribution) {
      if (val > 1) {
        Assertions.fail("No value should be above one.");
      } else if (val == 1) {
        oneValuesCnt++;
      } else if (val == 0.5) {
        halfValuesCnt++;
      } else {
        nonOneValuesSum += val;
      }
    }

    Assertions.assertEquals((((double) (100) / (double) (DateTimeConstants.MILLIS_PER_WEEK)) + 2) / 3, nonOneValuesSum / 2, 0.1);

    // Very unlikely but possible to fail if test is run exactly on the hour.

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - DateTimeConstants.HOURS_PER_DAY - 1, oneValuesCnt);
    Assertions.assertEquals(DateTimeConstants.HOURS_PER_DAY - 1, halfValuesCnt);
  }
}