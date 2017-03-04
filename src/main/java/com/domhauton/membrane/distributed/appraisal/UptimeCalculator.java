package com.domhauton.membrane.distributed.appraisal;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Seconds;

/**
 * Created by Dominic Hauton on 04/03/17.
 */
public class UptimeCalculator {
  private final DateTime firstMeasured;
  private final AtomicDoubleArray timesSeenAtHourOfWeek;

  private DateTime previousUpdateTime;

  public UptimeCalculator() {
    timesSeenAtHourOfWeek = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
    previousUpdateTime = DateTime.now();
    firstMeasured = DateTime.now();
  }

  public synchronized void updateUptime() {
    updateUptime(DateTime.now());
  }

  synchronized void updateUptime(DateTime dateTimeOfUptime) {
    boolean repeat = true;
    for (int i = 0; repeat && i < DateTimeConstants.HOURS_PER_WEEK; i++) { // ARTIFICIAL CAP FOR RUNAWAY.
      repeat = false;
      DateTime currentDateTime = dateTimeOfUptime;
      int hourOfWeekLast = previousUpdateTime.getHourOfDay() + (previousUpdateTime.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
      int hourOfWeekNow = currentDateTime.getHourOfDay() + (currentDateTime.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
      if (hourOfWeekLast != hourOfWeekNow) {
        currentDateTime = previousUpdateTime.plusHours(1).withTime(currentDateTime.getHourOfDay(), 0, 0, 0);
        repeat = true;
      }
      int secondsToAdd = Seconds.secondsBetween(previousUpdateTime, currentDateTime.toDateTime()).getSeconds();
      double percentageToAdd = secondsToAdd / DateTimeConstants.SECONDS_PER_HOUR;
      timesSeenAtHourOfWeek.addAndGet(hourOfWeekLast, percentageToAdd);
      previousUpdateTime = currentDateTime;
    }
  }
}
