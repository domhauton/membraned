package com.domhauton.membrane.distributed.appraisal;

import com.google.common.util.concurrent.AtomicDoubleArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class PeerAppraisal {
  private static final Logger LOGGER = LogManager.getLogger();

  private final String peerId;
  private final DateTime firstInteractionTime;
  private final AtomicDoubleArray timesSeenAtHourOfWeek;
  private long incompleteReports = 0;
  private long completeReports = 0;


  private Set<String> reportsReceived;
  private long reportsExpected;
  private DateTime countingForHour;


  PeerAppraisal(String peerId, DateTime firstInteractionTime) {
    this.peerId = peerId;
    this.firstInteractionTime = firstInteractionTime;
    timesSeenAtHourOfWeek = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
    reportsReceived = new HashSet<>();
  }

  public String getPeerId() {
    return peerId;
  }

  /**
   * Accept report from the peer that they are there.
   *
   * @param dateTimeSeen        Time the shard arrived
   * @param totalExpectedShards Total shard reports expect this hour.
   */
  synchronized void registerReport(DateTime dateTimeSeen, long totalExpectedShards) {
    registerReport(dateTimeSeen, totalExpectedShards, null);
  }

  /**
   * Report a new shard confirmation has arrived from peer.
   *
   * @param dateTimeSeen        Time the shard arrived
   * @param totalExpectedShards Total shard reports expect this hour.
   * @param shardId             Id of shard being confirmed.
   */
  synchronized void registerReport(DateTime dateTimeSeen, long totalExpectedShards, String shardId) {
    boolean newShardId = countHourlyReports(dateTimeSeen, shardId, totalExpectedShards);

    int hourOfWeek = dateTimeSeen.getHourOfDay() + (dateTimeSeen.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
    double shardPercentage = totalExpectedShards == 0 ? 1.0d : 1.0d / (double) totalExpectedShards;

    // Prevent users with required shards getting credit for empty reports
    shardPercentage = totalExpectedShards != 0 && !newShardId ? 0.0d : shardPercentage;

    registerReport(hourOfWeek, shardPercentage);
  }

  /**
   * Report a new shard is confirmed.
   * Adds the shard value to the correct portion of the distribution.
   *
   * @param hourOfWeek        hour of the week in the array to add to
   * @param shardPercentage   value to add for the hour of the week
   */
  private void registerReport(int hourOfWeek, double shardPercentage) {
    if (hourOfWeek < timesSeenAtHourOfWeek.length()) {
      timesSeenAtHourOfWeek.addAndGet(hourOfWeek, shardPercentage);
    } else {
      LOGGER.error("Invalid datetime passed for shard confirmed: {} < {}. {} hours per week expected.",
              hourOfWeek, timesSeenAtHourOfWeek.length(), DateTimeConstants.HOURS_PER_DAY);
    }
  }

  /**
   * Flush the received reports. To be done at the end of the hour if an early calculation is required.
   */
  synchronized void flushHourlyReportIntake() {
    if (countingForHour != null) {
      // Store values temporarily
      DateTime currentlyCountingForHour = countingForHour;
      long reportsCurrentlyExpected = reportsExpected;

      // Pretend the hour shifted forward

      countHourlyReports(countingForHour.plusHours(1), null, 0);

      // Reset the values

      countingForHour = currentlyCountingForHour;
      reportsExpected = reportsCurrentlyExpected;
    }
  }

  /**
   * Report (a) new report/s arriving at the given dateTime. Should never report past events.
   *
   * @param dateTime           Time the report arrived
   * @param shardId            Id of reported shard. Can be null.
   * @param newReportsExpected Number of reports expected this hour
   */
  private synchronized boolean countHourlyReports(DateTime dateTime, String shardId, long newReportsExpected) {
    DateTime reportForHour = dateTime.withTime(dateTime.hourOfDay().get(), 0, 0, 0); // Ceiling to hour
    if (countingForHour != null && reportForHour.isBefore(countingForHour)) {
      LOGGER.warn("Reported counted too late for [{}]. Ignoring.", peerId);
    } else {
      if (countingForHour != null && !reportForHour.equals(countingForHour)) {
        if (reportsExpected <= reportsReceived.size()) {
          completeReports++;
        } else {
          incompleteReports++;
        }
        reportsReceived = new HashSet<>();
      }
      countingForHour = reportForHour;
      reportsExpected = Math.max(reportsExpected, newReportsExpected);
      if (shardId != null && !reportsReceived.contains(shardId)) {
        reportsReceived.add(shardId);
        return true;
      }
    }
    return false;
  }

  /**
   * Get an array showing the percentage of time the peer was present in each hour of the week.
   *
   * @param dateTime Time the time now.
   * @return Array of hours and probabilities
   */
  double[] getShardReturnDistribution(DateTime dateTime) {
    long relationshipLengthMillis = dateTime.getMillis() - firstInteractionTime.getMillis();
    AtomicDoubleArray bestUptime = AppraisalUtils.calcTimeAtHourSlots(firstInteractionTime, relationshipLengthMillis);

    double[] result = new double[bestUptime.length()];
    for (int i = 0; i < bestUptime.length(); i++) {
      result[i] = bestUptime.get(i) == 0.0d ? 0.0d : timesSeenAtHourOfWeek.get(i) / bestUptime.get(i);
      result[i] = Math.min(result[i], 1.0d); // Clamp to 1.
    }
    return result;
  }

  /**
   * Percentage of hours in which a report was submitted, that all required reports were submitted.
   *
   * @return [0.0d-1.0d] 0.0d if success rate is zero
   */
  double getContractSuccessChance() {
    // Copy before use

    long tmpCompleteReports = completeReports;
    long totalReports = tmpCompleteReports + incompleteReports;

    // Chance is 1 if it doesn't exist

    return totalReports == 0 ? 1.0d : (double) tmpCompleteReports / (double) totalReports;
  }
}
