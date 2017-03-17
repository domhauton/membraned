package com.domhauton.membrane.distributed.appraisal;

import org.joda.time.DateTime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class AppraisalLedger implements Runnable {
  private final static int UPTIME_UPDATE_RATE = 2;

  private final ConcurrentHashMap<String, PeerAppraisal> appraisalMap;
  private final UptimeCalculator uptimeCalculator;
  private final ScheduledExecutorService executorService;

  public AppraisalLedger() {
    uptimeCalculator = new UptimeCalculator();
    appraisalMap = new ConcurrentHashMap<>();
    executorService = Executors.newSingleThreadScheduledExecutor();
  }

  public void registerPeerContact(String peerId, DateTime reportDateTime, int expectedShards, String shardId) {
    getPeerAppraisal(peerId).registerReport(reportDateTime, expectedShards, shardId);
  }

  public void registerPeerContact(String peerId, DateTime reportDateTime, int expectedShards) {
    getPeerAppraisal(peerId).registerReport(reportDateTime, expectedShards);
  }

  public double[] getUptime(String peerId) {
    return getPeerAppraisal(peerId).getShardReturnDistribution(DateTime.now());
  }

  public double[] getUptime() {
    return uptimeCalculator.getUptimeDistribution();
  }

  public double getPeerContractSuccessRate(String peerId) {
    return getPeerAppraisal(peerId).getContractSuccessChance();
  }

  public double getPeerRating(String peerId) {
    double[] myUptimeDistribution = getUptime();
    double[] peerUptimeDistribution = getUptime(peerId);
    double myUptimeTotal = 0.0d;
    double adjustedPeerUptimeTotal = 0.0d;
    for (int i = 0; i < myUptimeDistribution.length; i++) {
      myUptimeTotal += myUptimeDistribution[i];
      adjustedPeerUptimeTotal += Math.min(myUptimeDistribution[i], peerUptimeDistribution[i]);
    }
    return myUptimeTotal <= 0.0d ? 1.0d : adjustedPeerUptimeTotal / myUptimeTotal;
  }

  @Override
  public void run() {
    executorService.scheduleAtFixedRate(uptimeCalculator::updateUptime, 0, UPTIME_UPDATE_RATE, TimeUnit.MINUTES);
  }

  public void close() {
    executorService.shutdown();
  }

  private PeerAppraisal getPeerAppraisal(String peerId) {
    return appraisalMap.computeIfAbsent(peerId, id -> new PeerAppraisal(id, DateTime.now()));
  }
}
