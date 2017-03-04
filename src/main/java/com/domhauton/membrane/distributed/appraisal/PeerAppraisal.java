package com.domhauton.membrane.distributed.appraisal;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class PeerAppraisal {
  private static final Logger LOGGER = LogManager.getLogger();

  private final String peerId;
  private final DateTime firstInteractionTime;
  private final AtomicDoubleArray timesSeenAtHourOfWeek;
  private final AtomicLong brokenContractCount;
  private final AtomicLong completedContractCount;

  public PeerAppraisal(String peerId, DateTime firstInteractionTime) {
    this.peerId = peerId;
    this.firstInteractionTime = firstInteractionTime;
    timesSeenAtHourOfWeek = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
    brokenContractCount = new AtomicLong(0L);
    completedContractCount = new AtomicLong(0L);
  }

  public String getPeerId() {
    return peerId;
  }

  public void reportShardConfirmed(DateTime dateTimeSeen, long totalExpectedShards) {
    int hourOfWeek = dateTimeSeen.getHourOfDay() + (dateTimeSeen.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
    double shardPercentage = 1 / totalExpectedShards;
    reportShardConfirmed(hourOfWeek, shardPercentage);
  }

  void reportShardConfirmed(int hourOfWeek, double shardPercentage) {
    if (hourOfWeek < timesSeenAtHourOfWeek.length()) {
      timesSeenAtHourOfWeek.addAndGet(hourOfWeek, shardPercentage);
    } else {
      LOGGER.error("Invalid datetime passed for shard confirmed: {} < {}. {} hours per week expected.",
              hourOfWeek, timesSeenAtHourOfWeek.length(), DateTimeConstants.HOURS_PER_DAY);
    }
  }

  public void reportShardContractEnd() {
    brokenContractCount.incrementAndGet();
    completedContractCount.incrementAndGet();
  }
}
