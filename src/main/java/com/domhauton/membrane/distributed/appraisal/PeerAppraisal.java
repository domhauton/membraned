package com.domhauton.membrane.distributed.appraisal;

import com.domhauton.membrane.distributed.appraisal.files.PeerAppraisalSerializable;
import com.google.common.util.concurrent.AtomicDoubleArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Hours;

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
  private long lostBlocks = 0;

  private long totalLifetimeBlocks = 0;

  private Set<String> reportsReceived;
  private long reportsExpected;
  private DateTime countingForHour;


  PeerAppraisal(String peerId, DateTime firstInteractionTime) {
    this.peerId = peerId;
    this.firstInteractionTime = firstInteractionTime;
    countingForHour = firstInteractionTime.hourOfDay().roundFloorCopy();
    timesSeenAtHourOfWeek = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
    reportsReceived = new HashSet<>();
  }

  PeerAppraisal(String peerId, long firstInteractionTimeMillis, double[] timesSeenAtHourOfWeek, long incompleteReports, long completeReports, long lostBlocks, long totalLifetimeBlocks, Set<String> reportsReceived, long reportsExpected, long countingForHourMillis) {
    this.peerId = peerId;
    this.firstInteractionTime = new DateTime(Math.max(0, firstInteractionTimeMillis));
    this.timesSeenAtHourOfWeek = new AtomicDoubleArray(DateTimeConstants.HOURS_PER_WEEK);
    this.incompleteReports = incompleteReports;
    this.completeReports = completeReports;
    this.lostBlocks = lostBlocks;
    this.totalLifetimeBlocks = totalLifetimeBlocks;
    this.reportsReceived = new HashSet<>(reportsReceived);
    this.reportsExpected = reportsExpected;
    this.countingForHour = new DateTime(Math.max(0, countingForHourMillis));

    for (int i = 0; i < timesSeenAtHourOfWeek.length && i < timesSeenAtHourOfWeek.length; i++) {
      this.timesSeenAtHourOfWeek.set(i, timesSeenAtHourOfWeek[i]);
    }
  }

  public String getPeerId() {
    return peerId;
  }

  /**
   * Accept report from the peer that they are there.
   *
   * @param dateTimeSeen        Time the block arrived
   * @param totalExpectedBlocks Total block reports expect this hour.
   */
  synchronized void registerReport(DateTime dateTimeSeen, long totalExpectedBlocks) {
    registerReport(dateTimeSeen, totalExpectedBlocks, null);
  }

  /**
   * Report a new block confirmation has arrived from peer.
   *
   * @param dateTimeSeen        Time the block arrived
   * @param totalExpectedBlocks Total block reports expect this hour.
   * @param blockId             Id of block being confirmed.
   */
  synchronized void registerReport(DateTime dateTimeSeen, long totalExpectedBlocks, String blockId) {
    boolean newBlockId = countHourlyReports(dateTimeSeen, blockId, totalExpectedBlocks);

    int hourOfWeek = dateTimeSeen.getHourOfDay() + (dateTimeSeen.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
    double blockPercentage = totalExpectedBlocks <= 0 ? 1.0d : 1.0d / (double) totalExpectedBlocks;

    // Prevent users with required blocks getting credit for empty reports
    blockPercentage = totalExpectedBlocks > 0 && !newBlockId ? 0.0d : blockPercentage;

    registerReport(hourOfWeek, blockPercentage);
  }

  /**
   * Report a new block is confirmed.
   * Adds the block value to the correct portion of the distribution.
   *
   * @param hourOfWeek        hour of the week in the array to add to
   * @param blockPercentage   value to add for the hour of the week
   */
  private void registerReport(int hourOfWeek, double blockPercentage) {
    if (hourOfWeek < timesSeenAtHourOfWeek.length()) {
      timesSeenAtHourOfWeek.addAndGet(hourOfWeek, blockPercentage);
    } else {
      LOGGER.error("Invalid datetime passed for block confirmed: {} < {}. {} hours per week expected.",
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

  synchronized Set<String> getReportsReceived(DateTime dateTime, long reportsExpected) {
    registerReport(dateTime, reportsExpected);
    // Return a copy.
    return new HashSet<>(reportsReceived);
  }

  /**
   * Report (a) new report/s arriving at the given dateTime. Should never report past events.
   *
   * @param dateTime           Time the report arrived
   * @param blockId            Id of reported block. Can be null.
   * @param newReportsExpected Number of reports expected this hour
   */
  private synchronized boolean countHourlyReports(DateTime dateTime, String blockId, long newReportsExpected) {
    DateTime reportForHour = dateTime.hourOfDay().roundFloorCopy();
    if (countingForHour != null && reportForHour.isBefore(countingForHour)) {
      LOGGER.warn("Reported counted too late for [{}]. Ignoring.", peerId);
    } else {
      if (countingForHour != null && !reportForHour.equals(countingForHour)) {
        totalLifetimeBlocks += reportsExpected;
        if (reportsExpected <= reportsReceived.size()) {
          completeReports++;
        } else {
          incompleteReports++;
        }
        reportsReceived = new HashSet<>();
      }
      countingForHour = reportForHour;
      reportsExpected = Math.max(reportsExpected, newReportsExpected);
      if (blockId != null && !reportsReceived.contains(blockId)) {
        reportsReceived.add(blockId);
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
  double[] getBlockReturnDistribution(DateTime dateTime) {
    long relationshipLengthMillis = dateTime.getMillis() - firstInteractionTime.getMillis();
    AtomicDoubleArray bestUptime = AppraisalUtils.calcTimeAtHourSlots(firstInteractionTime, relationshipLengthMillis);

    double[] result = new double[bestUptime.length()];
    for (int i = 0; i < bestUptime.length(); i++) {
      result[i] = bestUptime.get(i) == 0.0d ? 0.0d : timesSeenAtHourOfWeek.get(i) / bestUptime.get(i);
      result[i] = Math.min(result[i], 1.0d); // Clamp at 1
      result[i] = Math.max(result[i], 0.0d); // Clamp at 0
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

  long getLostBlockCount() {
    return lostBlocks;
  }

  synchronized void addLostBlock(DateTime dateTimeLost, int totalExpectedBlocks) {
    lostBlocks++;

    int hourOfWeek = dateTimeLost.getHourOfDay() + (dateTimeLost.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
    double blockPercentage = totalExpectedBlocks <= 0 ? 1.0d : 1.0d / (double) totalExpectedBlocks;

    registerReport(hourOfWeek, -blockPercentage);
  }

  /**
   * Chance of peer losing stored block
   *
   * @return Chance [0.0,1.0]
   */
  double getBlockLosingRate() {
    double punishmentMultiplier = Math.min(DateTimeConstants.HOURS_PER_WEEK * 2, Math.abs(Hours.hoursBetween(firstInteractionTime, DateTime.now()).getHours()));
    punishmentMultiplier = Math.max(1.0, punishmentMultiplier);
    double chanceOfLosingBlocks = totalLifetimeBlocks <= 0 ? 0.0d : ((double) lostBlocks * punishmentMultiplier) / (double) totalLifetimeBlocks;
    // Clamp between 0.0d and 1.0d
    return Math.min(chanceOfLosingBlocks, 1.0d);
  }

  PeerAppraisalSerializable serialize() {
    return new PeerAppraisalSerializable(
        peerId,
        firstInteractionTime,
        timesSeenAtHourOfWeek,
        incompleteReports,
        completeReports,
        lostBlocks,
        totalLifetimeBlocks,
        reportsReceived,
        reportsExpected,
        countingForHour
    );
  }
}
