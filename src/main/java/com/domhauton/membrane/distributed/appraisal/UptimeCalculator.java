package com.domhauton.membrane.distributed.appraisal;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Hours;

import java.util.Arrays;

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

  synchronized void updateUptime() {
    updateUptime(DateTime.now());
  }

  synchronized void updateUptime(DateTime dateTimeOfUptime) {
    long millisBetweenTimes = dateTimeOfUptime.getMillis() - previousUpdateTime.getMillis();
    if (millisBetweenTimes > 0) {
      long millisToNextHour = previousUpdateTime.plusHours(1).withTime(previousUpdateTime.plusHours(1).getHourOfDay(), 0, 0, 0).getMillis() - previousUpdateTime.getMillis();
      int hourOfWeekLast = previousUpdateTime.getHourOfDay() + (previousUpdateTime.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
      if (millisToNextHour < millisBetweenTimes) {
        timesSeenAtHourOfWeek.addAndGet(hourOfWeekLast++, (double) millisToNextHour / (double) DateTimeConstants.MILLIS_PER_HOUR);
        long remainingMillis = millisBetweenTimes - millisToNextHour;

        long extraHours = remainingMillis / DateTimeConstants.MILLIS_PER_HOUR;
        for (int i = hourOfWeekLast; i < hourOfWeekLast + extraHours; i++) {
          timesSeenAtHourOfWeek.addAndGet(i % DateTimeConstants.HOURS_PER_WEEK, 1.0);
        }

        int finalIdx = (int) (hourOfWeekLast + extraHours) % DateTimeConstants.HOURS_PER_WEEK;
        timesSeenAtHourOfWeek.addAndGet(finalIdx, (double) (remainingMillis % DateTimeConstants.MILLIS_PER_HOUR) / (double) DateTimeConstants.MILLIS_PER_HOUR);

        // Consider over two hours
      } else {
        timesSeenAtHourOfWeek.addAndGet(hourOfWeekLast, (double) millisBetweenTimes / (double) DateTimeConstants.MILLIS_PER_HOUR);
      }
      previousUpdateTime = dateTimeOfUptime;
    }
  }

  double[] getUptimeDistribution() {
    return getUptimeDistribution(DateTime.now());
  }

  double[] getUptimeDistribution(DateTime atDateTime) {
    int totalHours = Hours.hoursBetween(firstMeasured, atDateTime).getHours();
    int fullWeeks = (totalHours / DateTimeConstants.HOURS_PER_WEEK);
    int extraHours = totalHours % DateTimeConstants.HOURS_PER_WEEK;

    int startHours = firstMeasured.getHourOfDay() + (firstMeasured.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;

    // Generate

    int[] timesSeen = new int[timesSeenAtHourOfWeek.length()];
    Arrays.fill(timesSeen, fullWeeks);
    for (int i = startHours; i < startHours + extraHours + 1; i++) {
      timesSeen[i % DateTimeConstants.HOURS_PER_WEEK]++;
    }


    double[] retPercentage = new double[timesSeenAtHourOfWeek.length()];
    for (int i = 0; i < retPercentage.length; i++) {
      retPercentage[i] = timesSeen[i] == 0 ? 0 : timesSeenAtHourOfWeek.get(i) / timesSeen[i];
    }

    return retPercentage;
  }
}
