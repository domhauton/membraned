package com.domhauton.membrane.distributed.appraisal.files;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.joda.time.DateTime;

import java.util.Set;

/**
 * Created by dominic on 12/04/17.
 */
public class PeerAppraisalSerializable {
  private String peerId;
  private long firstInteractionTimeMillis;
  private double[] timesSeenAtHourOfWeek;
  private long incompleteReports = 0;
  private long completeReports = 0;
  private long lostBlocks = 0;
  private long totalLifetimeBlocks = 0;
  private Set<String> reportsReceived;
  private long reportsExpected;
  private long countingForHourMillis;

  private PeerAppraisalSerializable() {
  } // Jackson ONLY

  public PeerAppraisalSerializable(String peerId, DateTime firstInteractionTime, AtomicDoubleArray timesSeenAtHourOfWeek, long incompleteReports, long completeReports, long lostBlocks, long totalLifetimeBlocks, Set<String> reportsReceived, long reportsExpected, DateTime countingForHour) {
    this.peerId = peerId;
    this.firstInteractionTimeMillis = firstInteractionTime.getMillis();
    this.timesSeenAtHourOfWeek = atomicDoubleArray2Array(timesSeenAtHourOfWeek);
    this.incompleteReports = incompleteReports;
    this.completeReports = completeReports;
    this.lostBlocks = lostBlocks;
    this.totalLifetimeBlocks = totalLifetimeBlocks;
    this.reportsReceived = reportsReceived;
    this.reportsExpected = reportsExpected;
    this.countingForHourMillis = countingForHour.getMillis();
  }

  public String getPeerId() {
    return peerId;
  }

  public long getFirstInteractionTimeMillis() {
    return firstInteractionTimeMillis;
  }

  public double[] getTimesSeenAtHourOfWeek() {
    return timesSeenAtHourOfWeek;
  }

  public long getIncompleteReports() {
    return incompleteReports;
  }

  public long getCompleteReports() {
    return completeReports;
  }

  public long getLostBlocks() {
    return lostBlocks;
  }

  public long getTotalLifetimeBlocks() {
    return totalLifetimeBlocks;
  }

  public Set<String> getReportsReceived() {
    return reportsReceived;
  }

  public long getReportsExpected() {
    return reportsExpected;
  }

  public long getCountingForHourMillis() {
    return countingForHourMillis;
  }

  private double[] atomicDoubleArray2Array(AtomicDoubleArray atomicDoubleArray) {
    double[] returnArray = new double[atomicDoubleArray.length()];
    for (int i = 0; i < atomicDoubleArray.length(); i++) {
      returnArray[i] = atomicDoubleArray.get(i);
    }
    return returnArray;
  }
}
