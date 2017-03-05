package com.domhauton.membrane.distributed.appraisal;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

/**
 * Created by Dominic Hauton on 04/03/17.
 */
public class UptimeCalculator {
  private final DateTime firstMeasured;
  private final AtomicDoubleArray timesSeenAtHourOfWeek;

  private DateTime previousUpdateTime;

  UptimeCalculator() {
    this(DateTime.now(), DateTime.now());
  }

  UptimeCalculator(DateTime firstMeasured, DateTime previousUpdateTime) {
    this.firstMeasured = firstMeasured;
    this.previousUpdateTime = previousUpdateTime;
    timesSeenAtHourOfWeek = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
  }

  synchronized DateTime updateUptime() {
    DateTime updatedDateTime = DateTime.now();
    updateUptime(updatedDateTime);
    return updatedDateTime;
  }

  synchronized void updateUptime(DateTime dateTimeOfUptime) {
    long millisBetweenTimes = dateTimeOfUptime.getMillis() - previousUpdateTime.getMillis();
    if (millisBetweenTimes > 0) {
      AppraisalUtils.calcTimeAtHourSlots(firstMeasured, millisBetweenTimes, timesSeenAtHourOfWeek);
      previousUpdateTime = dateTimeOfUptime;
    }
  }

  double[] getUptimeDistribution() {
    return getUptimeDistribution(DateTime.now());
  }

  double[] getUptimeDistribution(DateTime atDateTime) {
    AtomicDoubleArray timesSeen = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
    long millisBetweenTimes = atDateTime.getMillis() - firstMeasured.getMillis();
    if (millisBetweenTimes > 0) {
      AppraisalUtils.calcTimeAtHourSlots(firstMeasured, millisBetweenTimes, timesSeen);
    }

    double[] retPercentage = new double[timesSeenAtHourOfWeek.length()];
    for (int i = 0; i < retPercentage.length; i++) {
      retPercentage[i] = timesSeen.get(i) == 0 ? 0 : timesSeenAtHourOfWeek.get(i) / timesSeen.get(i);
      // Clamp results -> Double aliasing might result in drift.
      retPercentage[i] = Math.min(retPercentage[i], 1.0d);
    }

    return retPercentage;
  }

  public DateTime getFirstMeasured() {
    return firstMeasured;
  }
}
