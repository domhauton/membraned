package com.domhauton.membrane.network.pex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by dominic on 28/03/17.
 */

public class PexManager {
  static final String PEX_FILE_NAME = "pex.csv";
  static final String PEX_BACKUP_FILE_NAME = "pex.csv.bak";

  private final Logger logger = LogManager.getLogger();
  private final Path pexFolder;

  private PexLedger pexLedger;

  public PexManager(int maxLedgerSize, Path pexFolder) throws PexException {
    this.pexFolder = pexFolder;
    this.pexLedger = loadLedgerFromFile(pexFolder, maxLedgerSize);
  }

  public void addEntry(String peerId, String ip, int port, boolean isPublic) {
    logger.info("Adding PEX entry for peer [{}]. IP: '{}' Port: '{}' Public: '{}'", peerId, ip, port, isPublic);
    try {
      PexEntry newEntry = new PexEntry(ip, port, isPublic, DateTime.now());
      pexLedger.addPexEntry(peerId, newEntry);
    } catch (PexException e) {
      logger.error("Ignoring invalid PEX entry: {}", e.getMessage());
    }
  }

  public PexEntry getEntry(String peerId) throws PexException {
    return pexLedger.getPexEntry(peerId)
        .orElseThrow(() -> new PexException("Peer [" + peerId + "] not found in PEX ledger"));
  }

  public Set<String> getAvailablePexPeers() {
    return pexLedger.availableHosts();
  }

  void saveLedger() throws PexException {
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

  PexLedger loadLedgerFromFile(Path pexFolder, int maxLedgerSize) throws PexException {
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
}
