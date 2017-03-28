package com.domhauton.membrane.network.pex;


import org.apache.commons.collections4.map.LinkedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 28/03/17.
 */
public class PexLedger {
  private final static Logger LOGGER = LogManager.getLogger();
  private final static String ENTRY_SEP = ",";

  private final int maxLedgerSize;
  private final LinkedMap<String, PexEntry> pexRecord;

  PexLedger(int ledgerSize) {
    this.maxLedgerSize = ledgerSize;
    this.pexRecord = new LinkedMap<>();
  }

  synchronized void addPexEntry(String userId, PexEntry pexEntry) {
    pexRecord.put(userId, pexEntry);
    while (pexRecord.size() > maxLedgerSize) {
      String keyIterator = pexRecord.firstKey();
      pexRecord.remove(keyIterator);
    }
  }

  public synchronized Optional<PexEntry> getPexEntry(String userId) {
    return Optional.ofNullable(pexRecord.get(userId));
  }

  public synchronized Set<String> availableHosts() {
    return pexRecord.keySet();
  }

  synchronized String serialize() {
    return pexRecord.entrySet().stream()
        .map(entry -> entry.getKey() + ENTRY_SEP + entry.getValue().serialize())
        .collect(Collectors.joining("\n"));
  }

  static PexLedger deserialize(Collection<String> ledgerFile, int maxLedgerSize) {
    PexLedger pexLedger = new PexLedger(maxLedgerSize);

    for (String entry : ledgerFile) {
      try {
        String[] splitEntry = entry.split(ENTRY_SEP, 1);
        if (splitEntry.length == 2) {
          PexEntry pexEntry = PexEntry.deserialize(splitEntry[1]);
          pexLedger.addPexEntry(splitEntry[0], pexEntry);
        } else {
          LOGGER.warn("Pex entry must be of form 'userId,entry': {}", entry);
        }
        PexEntry.deserialize(entry);
      } catch (PexException e) {
        LOGGER.warn("Unable to parse PEX entry. {}", e.getMessage());
      }
    }
    return pexLedger;
  }
}
