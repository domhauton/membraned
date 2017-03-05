package com.domhauton.membrane.distributed.appraisal;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

/**
 * Created by Dominic Hauton on 05/03/17.
 */
abstract class AppraisalUtils {

  static AtomicDoubleArray calcTimeAtHourSlots(DateTime startTime, long millisBetweenTimes) {
    return calcTimeAtHourSlots(startTime, millisBetweenTimes, new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK));
  }

  static AtomicDoubleArray calcTimeAtHourSlots(DateTime startTime, long millisBetweenTimes, AtomicDoubleArray addToArray) {
    if (millisBetweenTimes > 0) {
      long millisToNextHour = startTime.plusHours(1).withTime(startTime.plusHours(1).getHourOfDay(), 0, 0, 0).getMillis() - startTime.getMillis();
      int hourOfWeekLast = startTime.getHourOfDay() + (startTime.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
      if (millisToNextHour < millisBetweenTimes) {
        addToArray.addAndGet(hourOfWeekLast++, (double) millisToNextHour / (double) DateTimeConstants.MILLIS_PER_HOUR);
        long remainingMillis = millisBetweenTimes - millisToNextHour;

        long extraHours = remainingMillis / DateTimeConstants.MILLIS_PER_HOUR;
        for (int i = hourOfWeekLast; i < hourOfWeekLast + extraHours; i++) {
          addToArray.addAndGet(i % DateTimeConstants.HOURS_PER_WEEK, 1.0);
        }

        int finalIdx = (int) (hourOfWeekLast + extraHours) % DateTimeConstants.HOURS_PER_WEEK;
        addToArray.addAndGet(finalIdx, (double) (remainingMillis % DateTimeConstants.MILLIS_PER_HOUR) / (double) DateTimeConstants.MILLIS_PER_HOUR);

        // Consider over two hours
      } else {
        addToArray.addAndGet(hourOfWeekLast, (double) millisBetweenTimes / (double) DateTimeConstants.MILLIS_PER_HOUR);
      }
    }
    return addToArray;
  }
}
