package com.domhauton.membrane.distributed.appraisal;

import com.domhauton.membrane.distributed.appraisal.files.PeerAppraisalFile;
import com.domhauton.membrane.distributed.appraisal.files.PeerAppraisalSerializable;
import com.domhauton.membrane.distributed.appraisal.files.UptimeSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class AppraisalLedger implements Runnable, Closeable {
  private final Logger logger = LogManager.getLogger();

  private final static String FILE_NAME = "appraisal.yml";
  private final static int UPTIME_UPDATE_RATE = 2;

  private final ConcurrentHashMap<String, PeerAppraisal> appraisalMap;
  private final UptimeCalculator uptimeCalculator;
  private final ScheduledExecutorService executorService;

  private final Path fullPersistPath;

  public AppraisalLedger(Path basePath) throws AppraisalException {
    appraisalMap = new ConcurrentHashMap<>();
    executorService = Executors.newSingleThreadScheduledExecutor();

    if (!basePath.toFile().exists()) {
      try {
        Files.createDirectories(basePath);
      } catch (IOException e) {
        logger.error("Could not create non-existent base path");
        throw new AppraisalException("Could not create base path.", e);
      }
    }

    fullPersistPath = Paths.get(basePath.toString() + File.separator + FILE_NAME);

    // Load from File if exists

    if (fullPersistPath.toFile().exists()) {
      PeerAppraisalFile peerAppraisalFile = readAppraisals(fullPersistPath);
      peerAppraisalFile2Appraisals(peerAppraisalFile)
          .forEach(x -> appraisalMap.put(x.getPeerId(), x));
      uptimeCalculator = peerAppraisalFile2Uptime(peerAppraisalFile);
    } else {
      uptimeCalculator = new UptimeCalculator();
    }
  }

  public void registerPeerContact(String peerId, DateTime reportDateTime, int expectedShards, String shardId) {
    getPeerAppraisal(peerId).registerReport(reportDateTime, expectedShards, shardId);
    uptimeCalculator.updateUptime(reportDateTime);
  }

  public void registerPeerContact(String peerId, DateTime reportDateTime, int expectedShards) {
    getPeerAppraisal(peerId).registerReport(reportDateTime, expectedShards);
    uptimeCalculator.updateUptime(reportDateTime);
  }

  public void registerLostBlock(String peerId, DateTime lostDateTime, int expectedShards) {
    getPeerAppraisal(peerId).addLostBlock(lostDateTime, expectedShards);
  }

  public double getPeerRating(String peerId) {
    return getPeerRating(peerId, DateTime.now());
  }

  /**
   * The uptime overlap with the peer, augmented by fulfillment rate.
   *
   * @param peerId
   * @return
   */
  double getPeerRating(String peerId, DateTime atTime) {
    PeerAppraisal peerAppraisal = getPeerAppraisal(peerId);

    double[] myUptimeDistribution = uptimeCalculator.getUptimeDistribution(atTime);
    double[] peerUptimeDistribution = peerAppraisal.getBlockReturnDistribution(atTime);
    double myUptimeTotal = 0.0d;
    double adjustedPeerUptimeTotal = 0.0d;
    for (int i = 0; i < myUptimeDistribution.length; i++) {
      myUptimeTotal += myUptimeDistribution[i];
      adjustedPeerUptimeTotal += Math.min(myUptimeDistribution[i], peerUptimeDistribution[i]);
    }

    // Now adjust rating according to fulfillment rate.

    double rating = myUptimeTotal <= 0.0d ? 1.0d : adjustedPeerUptimeTotal / myUptimeTotal;
    rating *= peerAppraisal.getContractSuccessChance();
    rating = Math.max(0.0d, rating);

    // Multiply by (inverted) block losing rate

    rating *= (1.0d - peerAppraisal.getBlockLosingRate());
    return rating;
  }

  @Override
  public void run() {
    executorService.scheduleAtFixedRate(uptimeCalculator::updateUptime, 0, UPTIME_UPDATE_RATE, TimeUnit.MINUTES);
    executorService.scheduleAtFixedRate(() -> {
      try {
        writeAppraisals(fullPersistPath);
      } catch (AppraisalException e) {
        logger.error("Failed to complete scheduled Appraisal persist. {}", e.getMessage());
      }
    }, 0, UPTIME_UPDATE_RATE, TimeUnit.MINUTES);
  }

  public void close() {
    try {
      writeAppraisals(fullPersistPath);
    } catch (AppraisalException e) {
      logger.error("Failed to complete Appraisal persist on Ledger lose. {}", e.getMessage());
    }
    executorService.shutdown();
  }

  private PeerAppraisal getPeerAppraisal(String peerId) {
    return appraisalMap.computeIfAbsent(peerId, id -> new PeerAppraisal(id, DateTime.now()));
  }


  private final static ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private synchronized PeerAppraisalFile readAppraisals(Path path) throws AppraisalException {
    try {
      logger.info("Reading peer appraisals from file. [{}]", path);
      return MAPPER.readValue(path.toFile(), PeerAppraisalFile.class);
    } catch (IOException e) {
      logger.error("Reading peer appraisals from file failed. [{}]", path);
      logger.debug(e);
      throw new AppraisalException("Failed to read appraisals from file. " + e.getMessage());
    }
  }

  private UptimeCalculator peerAppraisalFile2Uptime(PeerAppraisalFile peerAppraisalFile) {
    UptimeSerializable uptimeSerializable = peerAppraisalFile.getUptime();
    return new UptimeCalculator(uptimeSerializable.getFirstInteractionTimeMillis(), uptimeSerializable.getPreviousUpdateTimeMillis(), uptimeSerializable.getTimesSeenAtHourOfWeek());
  }

  private List<PeerAppraisal> peerAppraisalFile2Appraisals(PeerAppraisalFile peerAppraisalFile) {
    List<PeerAppraisalSerializable> peerAppraisals = peerAppraisalFile.getPeerAppraisals();
    return peerAppraisals.stream()
        .map(x -> new PeerAppraisal(x.getPeerId(), x.getFirstInteractionTimeMillis(), x.getTimesSeenAtHourOfWeek(), x.getIncompleteReports(), x.getCompleteReports(), x.getLostBlocks(), x.getTotalLifetimeBlocks(), x.getReportsReceived(), x.getReportsExpected(), x.getCountingForHourMillis()))
        .collect(Collectors.toList());
  }

  void writeAppraisals() throws AppraisalException {
    writeAppraisals(fullPersistPath);
  }

  private synchronized void writeAppraisals(Path path) throws AppraisalException {
    List<PeerAppraisalSerializable> peerAppraisals = appraisalMap.values().stream()
        .map(PeerAppraisal::serialize)
        .collect(Collectors.toList());
    UptimeSerializable uptimeSerial = uptimeCalculator.serialize();
    PeerAppraisalFile peerAppraisalFile = new PeerAppraisalFile(peerAppraisals, uptimeSerial);
    try {
      logger.info("Storing {} peer appraisals to file. [{}]", peerAppraisals.size(), path);
      Path tmpPath = Paths.get(fullPersistPath.toString() + ".tmp");
      Files.deleteIfExists(tmpPath);
      MAPPER.writeValue(tmpPath.toFile(), peerAppraisalFile);
      Files.move(tmpPath, fullPersistPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      logger.error("Failed to store peer appraisals to file. [{}] {}", path, e.getMessage());
      throw new AppraisalException("Failed to open file. ", e);
    }
  }
}
