package com.domhauton.membrane.network.pex;

import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messages.PexAdvertisement;
import com.domhauton.membrane.network.messages.PexQueryRequest;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by dominic on 28/03/17.
 */

public class PexManager {
  static final String PEX_FILE_NAME = "pex.csv";
  static final String PEX_BACKUP_FILE_NAME = "pex.csv.bak";
  private static final int PEX_ENTRY_TIMEOUT_MINS = 15;

  private final Logger logger = LogManager.getLogger();
  private final Path pexFolder;

  private PexLedger pexLedger;
  private PexLedger unconfirmedLedger;

  public PexManager(int maxLedgerSize, Path pexFolder) throws PexException {
    this.pexFolder = pexFolder;
    this.pexLedger = loadLedgerFromFile(pexFolder, maxLedgerSize);
    this.unconfirmedLedger = new PexLedger(maxLedgerSize);
  }

  public Optional<PexEntry> addEntry(String peerId, String ip, int port, boolean isPublic, DateTime dateTime, byte[] signature) {
    logger.info("Adding PEX entry for peer [{}]. IP: '{}' Port: '{}' Public: '{}'", peerId, ip, port, isPublic);
    try {
      PexEntry newEntry = new PexEntry(ip, port, isPublic, dateTime, signature);
      Optional<PexEntry> oldEntryOptional = pexLedger.getPexEntry(peerId);

      if (!oldEntryOptional.isPresent() || oldEntryOptional.get().getLastUpdateDateTime().isBefore(dateTime)) {
        pexLedger.addPexEntry(peerId, newEntry);
        return Optional.of(newEntry);
      } else {
        logger.error("Ignoring outdated PEX entry.");
        return Optional.empty();
      }
    } catch (PexException e) {
      logger.error("Ignoring invalid PEX entry: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public void addUnconfirmedEntry(String ip, int port) {
    logger.trace("Adding unconfirmed PEX entry. IP: '{}' Port: '{}'", ip, port);
    try {
      PexEntry newEntry = new PexEntry(ip, port, true, DateTime.now(), new byte[0]);
      // Use IP to filter spam
      unconfirmedLedger.addPexEntry(ip, newEntry);
    } catch (PexException e) {
      logger.trace("Ignoring invalid unconfirmed PEX entry: {}", e.getMessage());
    }
  }

  public Set<PexEntry> getPublicEntries(int limit) {
    return getPublicEntries(limit, Collections.emptySet());
  }

  public Set<PexEntry> getPublicEntries(int limit, Set<String> ignorePeers) {
    DateTime oldestPermittedTime = DateTime.now().minusMinutes(PEX_ENTRY_TIMEOUT_MINS);
    return pexLedger.getPexEntries().stream()
        .filter(pexEntry -> !ignorePeers.contains(pexEntry.getKey()))
        .map(Map.Entry::getValue)
        .filter(PexEntry::isPublicEntry)
        .filter(pexEntry -> pexEntry.getLastUpdateDateTime().isAfter(oldestPermittedTime))
        .sorted((o1, o2) -> DateTimeComparator.getInstance().reversed().compare(o1.getLastUpdateDateTime(), o2.getLastUpdateDateTime()))
        .limit(limit)
        .collect(Collectors.toSet());
  }

  public PexEntry getEntry(String peerId) throws PexException {
    return pexLedger.getPexEntry(peerId)
        .orElseThrow(() -> new PexException("Peer [" + peerId + "] not found in PEX ledger"));
  }

  public Set<String> getAvailablePexPeers() {
    return pexLedger.availableHosts();
  }

  public Set<Map.Entry<String, PexEntry>> getPexEntries() {
    return pexLedger.getPexEntries();
  }

  public void saveLedger() throws PexException {
    logger.info("Persisting PEX ledger to file.");
    Path pexFilePath = Paths.get(pexFolder.toString() + File.separator + PEX_FILE_NAME);
    Path pexBackupFilePath = Paths.get(pexFolder.toString() + File.separator + PEX_BACKUP_FILE_NAME);

    // Move current persisted record to the backup

    try {
      if (pexFilePath.toFile().exists()) { // Recover from backup
        Files.move(pexFilePath, pexBackupFilePath, REPLACE_EXISTING, ATOMIC_MOVE);
      }
    } catch (IOException e) {
      logger.warn("Failed to move main to backup file. [{}] -> [{}]", pexBackupFilePath, pexFilePath);
      throw new PexException("Failed to move main to backup file in folder " + pexFolder.toString(), e);
    }

    // Save ledger and remove the backup upon success

    try {
      String newFileContents = pexLedger.serialize();
      Files.write(pexFilePath, newFileContents.getBytes());
      Files.deleteIfExists(pexBackupFilePath); // Complete write. If backup still exists it will be used instead.
    } catch (IOException e) {
      logger.error("Pex persistence failed. {}", e.getMessage());
      throw new PexException("Pex persistence failed.", e);
    }
  }

  private PexLedger loadLedgerFromFile(Path pexFolder, int maxLedgerSize) throws PexException {
    Path pexFilePath = Paths.get(pexFolder.toString() + File.separator + PEX_FILE_NAME);
    Path pexBackupFilePath = Paths.get(pexFolder.toString() + File.separator + PEX_BACKUP_FILE_NAME);

    logger.info("Attempting to load PEX from [{}]", pexFilePath);

    // Recover from backup if it exists

    if (pexBackupFilePath.toFile().exists()) { // Recover from backup
      logger.info("Found PEX backup at [{}]. Using backup instead.", pexFilePath);
      try {
        Files.move(pexBackupFilePath, pexFilePath, REPLACE_EXISTING, ATOMIC_MOVE);
      } catch (IOException e) {
        logger.warn("Failed to move backup to main file. [{}] -> [{}]", pexBackupFilePath, pexFilePath);
        throw new PexException("Failed to move backup to main file in folder " + pexFolder.toString(), e);
      }
    }

    if (pexFilePath.toFile().exists()) { // Actually load from file.
      try {
        logger.trace("Reading PEX from [{}]", pexFilePath);
        List<String> pexFile = Files.readAllLines(pexFilePath);
        return PexLedger.deserialize(pexFile, maxLedgerSize);
      } catch (IOException e) {
        throw new PexException("Failed to read pex info from file [" + pexFilePath.toString() + "]");
      }
    } else { // Just return a new one.
      return new PexLedger(maxLedgerSize);
    }
  }

  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * Attempt connections to provided public PEX entries.
   */
  public void connectToPublicPeersInPex(ConnectionManager connectionManager, int requiredConnections) {
    // Shouldn't be less than zero. Clamping.
    requiredConnections = Math.max(0, requiredConnections);
    DateTime oldestPermittedTime = DateTime.now().minusMinutes(PEX_ENTRY_TIMEOUT_MINS);
    long totalDialledPublicPeers = unconfirmedLedger.getPexEntries()
        .stream()
        .map(Map.Entry::getValue)
        .filter(pexEntry -> pexEntry.getLastUpdateDateTime().isAfter(oldestPermittedTime))
        .sorted((o1, o2) -> DateTimeComparator.getInstance().reversed().compare(o1.getLastUpdateDateTime(), o2.getLastUpdateDateTime()))
        .limit(requiredConnections)
        .peek(x -> connectionManager.connectToPeer(x.getAddress(), x.getPort()))
        .count();
    LOGGER.info("Connecting to {} of required peers in public pex.", totalDialledPublicPeers, requiredConnections);
  }

  /**
   * Send updated PEX information to all peers.
   *
   * @param isPublic should the PEX information be re-distributed
   */
  public static void sendPexUpdate(ExternalAddress externalAddress, Collection<Peer> connectedPeers, boolean isPublic) {
    LOGGER.info("Sending {} PEX updates to {} peers.", isPublic ? "Public" : "Non-Public", connectedPeers.size());
    for (Peer peer : connectedPeers) {
      try {
        PexAdvertisement pexAdvertisement = new PexAdvertisement(externalAddress.getIpAddress(), externalAddress.getPort(), isPublic, DateTime.now());
        peer.sendPeerMessage(pexAdvertisement);
      } catch (PeerException e) {
        LOGGER.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
      }
    }
  }

  /**
   * Requests all connected peers for PEX information
   */
  public static void requestPexInformation(Collection<Peer> peers, Set<String> contractedPeers, boolean searchingForNewPeers) {
    LOGGER.info("Requesting peers for PEX update.");
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractedPeers);
    peers.forEach(x -> lostPeers.remove(x.getUid()));

    if (!lostPeers.isEmpty() || searchingForNewPeers) {
      LOGGER.info("Sending a PEX request for {} known {}peers", lostPeers.size(), searchingForNewPeers ? "public" : "");
      PexQueryRequest pexQueryRequest = new PexQueryRequest(lostPeers, searchingForNewPeers);
      for (Peer peer : peers) {
        try {
          peer.sendPeerMessage(pexQueryRequest);
        } catch (PeerException e) {
          LOGGER.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
        }
      }
    }
  }
}
