package com.domhauton.membrane.distributed.appraisal;

import com.domhauton.membrane.distributed.appraisal.files.UptimeSerializable;
import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;

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

  UptimeCalculator(long firstMeasuredMillis, long previousUpdateTimeMillis, double[] timesSeenArray) {
    this.firstMeasured = new DateTime(Math.max(0, firstMeasuredMillis));
    this.previousUpdateTime = new DateTime(Math.max(0, previousUpdateTimeMillis));
    timesSeenAtHourOfWeek = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);

    for (int i = 0; i < timesSeenArray.length && i < this.timesSeenAtHourOfWeek.length(); i++) {
      this.timesSeenAtHourOfWeek.set(i, timesSeenArray[i]);
    }
  }

  synchronized DateTime updateUptime() {
    DateTime updatedDateTime = DateTime.now();
    updateUptime(updatedDateTime);
    return updatedDateTime;
  }

  synchronized void updateUptime(DateTime dateTimeOfUptime) {
    if (previousUpdateTime.isBefore(dateTimeOfUptime)) {
      int hourOfWeekLast = AppraisalUtils.getHourOfWeek(previousUpdateTime);
      int hourOfWeekNow = AppraisalUtils.getHourOfWeek(dateTimeOfUptime);
      if (Days.daysBetween(previousUpdateTime, dateTimeOfUptime).getDays() > 0 || hourOfWeekLast != hourOfWeekNow) {
        timesSeenAtHourOfWeek.addAndGet(hourOfWeekNow, 1);
      }
      previousUpdateTime = dateTimeOfUptime;
    }
  }

  double[] getUptimeDistribution() {
    return getUptimeDistribution(DateTime.now());
  }

  double[] getUptimeDistribution(DateTime atDateTime) {
    AtomicDoubleArray bestUptime = AppraisalUtils.calcBestUptime(firstMeasured, atDateTime);

    double[] retPercentage = new double[timesSeenAtHourOfWeek.length()];
    for (int i = 0; i < retPercentage.length; i++) {
      retPercentage[i] = bestUptime.get(i) == 0 ? 0 : (timesSeenAtHourOfWeek.get(i) / bestUptime.get(i));
      // Clamp results -> Double aliasing might result in drift.
      retPercentage[i] = Math.min(retPercentage[i], 1.0d);
    }

    return retPercentage;
  }

  UptimeSerializable serialize() {
    return new UptimeSerializable(firstMeasured, previousUpdateTime, timesSeenAtHourOfWeek);
  }
}
