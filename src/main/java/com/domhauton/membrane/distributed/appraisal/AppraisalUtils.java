package com.domhauton.membrane.distributed.appraisal;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Hours;

/**
 * Created by Dominic Hauton on 05/03/17.
 */
abstract class AppraisalUtils {

  static AtomicDoubleArray calcBestUptime(DateTime startTime, DateTime end) {
    int relationshipLengthHours = Hours.hoursBetween(startTime, end).getHours();
    int relationshipWeeks = relationshipLengthHours / DateTimeConstants.HOURS_PER_WEEK;
    AtomicDoubleArray atomicDoubleArray = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
    if (startTime.isAfter(end)) {
      return atomicDoubleArray;
    }

    // Fill for all weeks

    for (int i = 0; i < DateTimeConstants.HOURS_PER_WEEK; i++) {
      atomicDoubleArray.set(i, relationshipWeeks);
    }

    // Fill extra hours

    int relationshipExtraHours = relationshipLengthHours % DateTimeConstants.HOURS_PER_WEEK;
    int startHourOfWeek = getHourOfWeek(startTime);

    for (int i = 0; i < relationshipExtraHours; i++) {
      int currentHour = (i + startHourOfWeek) % DateTimeConstants.HOURS_PER_WEEK;
      atomicDoubleArray.getAndAdd(currentHour, 1);
    }

    return atomicDoubleArray;
  }

  static int getHourOfWeek(DateTime dateTime) {
    return dateTime.getHourOfDay() + (dateTime.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
  }
}
