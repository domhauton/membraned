package com.domhauton.membrane.distributed.appraisal;

import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by dominic on 30/04/17.
 */
class AppraisalSimulation {

  private Path basePath;
  private AppraisalLedger appraisalLedger;
  private SimulatedPeer me;
  private Set<SimulatedPeer> goodPeers;
  private Set<SimulatedPeer> midPeers;
  private Set<SimulatedPeer> badPeers;

  private final int weekdayStart = 18;
  private final int weekdayEnd = 23;
  private final int weekendStart = 9;
  private final int weekendEnd = 23;

  private DateTime currentDateTime = DateTime.now().plusMinutes(5);

  @BeforeEach
  void setUp() throws Exception {
    basePath = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));
    appraisalLedger = new AppraisalLedger(basePath);
    me = new SimulatedPeer("me", weekdayStart, weekdayEnd, weekendStart, weekendEnd);
    //me = new SimulatedPeer("me", 0, 24, 0, 24);
    setupGood(1);
    setupAveragePeers(3);
    setupBadPeers(3);
  }

  @Test
  void runSimulation() {
    for (int i = 0; i < DateTimeConstants.HOURS_PER_WEEK; i++) {
      stepSimulation();
    }
    System.out.println(Arrays.toString(appraisalLedger.getUptimeCalculator().getUptimeDistribution(currentDateTime)));
    me.printAppraisalCSV();
    getSimulatedPeers().forEach(SimulatedPeer::printAppraisalCSV);
  }

  private void setupGood(int count) {
    goodPeers = IntStream.range(0, count)
        .boxed()
        .map(x -> new SimulatedPeer("peer-good-" + x, 0, 24, 0, 24))
        .collect(Collectors.toSet());
  }

  private void setupAveragePeers(int count) {
    midPeers = IntStream.range(0, count)
        .boxed()
        .map(x -> new SimulatedPeer("peer-mid-" + x, weekdayStart + 2, weekdayEnd, weekendStart, weekendEnd - 4))
        .collect(Collectors.toSet());
  }

  private void setupBadPeers(int count) {
    badPeers = IntStream.range(0, count)
        .boxed()
        .map(x -> new SimulatedPeer("peer-bad-" + x, 1, 2, weekendStart, weekendEnd - 8))
        .collect(Collectors.toSet());
  }

  private void stepSimulation() {
    if (me.isOnline(currentDateTime)) {
      System.out.println("ONLINE");
      appraisalLedger.getUptimeCalculator().updateUptime(currentDateTime);
      for (SimulatedPeer simulatedPeer : getSimulatedPeers()) {
        if (simulatedPeer.isOnline(currentDateTime)) {
          appraisalLedger.registerPeerContact(simulatedPeer.getName(), currentDateTime, 1, "block-1");
          appraisalLedger.registerPeerContact(simulatedPeer.getName(), currentDateTime, 2, "block-2");
        }
        simulatedPeer.addAppraisal(appraisalLedger.getPeerRating(simulatedPeer.getName(), currentDateTime));
      }
      me.addAppraisal(calcMinPermitted());
    } else {
      for (SimulatedPeer simulatedPeer : getSimulatedPeers()) {
        simulatedPeer.addAppraisal(appraisalLedger.getPeerRating(simulatedPeer.getName(), currentDateTime));
      }
      me.addAppraisal(calcMinPermitted());
    }

    currentDateTime = currentDateTime.plusHours(1);
  }

  private Double calcMinPermitted() {
    Set<String> allPeers = getPeerNames();

    Map<String, Double> peerReliabilityMap = allPeers.stream()
        .collect(Collectors.toMap(x -> x, x -> appraisalLedger.getPeerRating(x, currentDateTime)));

    double mean = peerReliabilityMap.values().stream()
        .mapToDouble(x -> x)
        .average()
        .orElse(0.0d);
    double var = peerReliabilityMap.values().stream()
        .mapToDouble(x -> (x - mean))
        .map(x -> x * x)
        .average()
        .orElse(0.0);
    double sd = Math.sqrt(var);
    return Math.max(0.0, mean - sd);
  }

  private Set<String> getPeerNames() {
    Set<String> allPeers = new HashSet<>();
    allPeers.addAll(goodPeers.stream().map(SimulatedPeer::getName).collect(Collectors.toSet()));
    allPeers.addAll(midPeers.stream().map(SimulatedPeer::getName).collect(Collectors.toSet()));
    allPeers.addAll(badPeers.stream().map(SimulatedPeer::getName).collect(Collectors.toSet()));
    return allPeers;
  }

  private Set<SimulatedPeer> getSimulatedPeers() {
    Set<SimulatedPeer> allPeers = new HashSet<>();
    allPeers.addAll(goodPeers);
    allPeers.addAll(midPeers);
    allPeers.addAll(badPeers);
    return allPeers;
  }

  @AfterEach
  void tearDown() throws Exception {
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath);
  }
}