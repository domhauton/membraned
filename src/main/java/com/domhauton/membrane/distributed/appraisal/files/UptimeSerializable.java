package com.domhauton.membrane.distributed.appraisal.files;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;

/**
 * Created by dominic on 13/04/17.
 */
public class UptimeSerializable {
  private long firstInteractionTimeMillis;
  private long previousUpdateTimeMillis;
  private double[] timesSeenAtHourOfWeek;

  private UptimeSerializable() {
  } // Jackson ONLY

  public UptimeSerializable(DateTime firstInteractionTimeMillis, DateTime previousUpdateTimeMillis, AtomicDoubleArray timesSeenAtHourOfWeek) {
    this.firstInteractionTimeMillis = firstInteractionTimeMillis.getMillis();
    this.previousUpdateTimeMillis = previousUpdateTimeMillis.getMillis();
    this.timesSeenAtHourOfWeek = atomicDoubleArray2Array(timesSeenAtHourOfWeek);
  }

  public long getFirstInteractionTimeMillis() {
    return firstInteractionTimeMillis;
  }

  public long getPreviousUpdateTimeMillis() {
    return previousUpdateTimeMillis;
  }

  public double[] getTimesSeenAtHourOfWeek() {
    return timesSeenAtHourOfWeek;
  }

  private double[] atomicDoubleArray2Array(AtomicDoubleArray atomicDoubleArray) {
    double[] returnArray = new double[atomicDoubleArray.length()];
    for (int i = 0; i < atomicDoubleArray.length(); i++) {
      returnArray[i] = atomicDoubleArray.get(i);
    }
    return returnArray;
  }
}
