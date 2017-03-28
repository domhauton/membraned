package com.domhauton.membrane.network.pex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardCopyOption.*;

/**
 * Created by dominic on 28/03/17.
 */
public class PexManager {
  private static final String PEX_FILE_NAME = "pex.csv";
  private static final String PEX_BACKUP_FILE_NAME = "pex.csv.bak";

  private final Logger logger = LogManager.getLogger();
  private final int maxLedgerSize;
  private final Path pexFolder;

  private PexLedger pexLedger;

  public PexManager(int maxLedgerSize, Path pexFolder) {
    this.maxLedgerSize = maxLedgerSize;
    this.pexFolder = pexFolder;

  }

  public void addEntry(String peerId, String ip, int port, boolean isPublic) {

  }

  PexLedger loadLedgerFromFile(Path pexFolder, int maxLedgerSize) throws PexException {
    Path pexFilePath = Paths.get(pexFolder.toString() + File.separator + PEX_FILE_NAME);
    Path pexBackupFilePath = Paths.get(pexFolder.toString() + File.separator + PEX_BACKUP_FILE_NAME);

    // Recover from backup if it exists

    try {
      if (pexBackupFilePath.toFile().exists()) { // Recover from backup
        Files.move(pexBackupFilePath, pexFilePath, REPLACE_EXISTING, ATOMIC_MOVE, COPY_ATTRIBUTES);
      }
    } catch (IOException e) {
      logger.warn("Failed to move backup to main file. [{}] -> [{}]", pexBackupFilePath, pexFilePath);
      throw new PexException("Failed to move backup to main file in folder " + pexFolder.toString(), e);
    }

    if (pexFilePath.toFile().exists()) { // Actually load from file.
      try {
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
