package com.domhauton.membrane.network.pex;


import org.apache.commons.collections4.map.LinkedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;
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

  synchronized Optional<PexEntry> getPexEntry(String userId) {
    return Optional.ofNullable(pexRecord.get(userId));
  }

  synchronized Set<String> availableHosts() {
    return pexRecord.keySet();
  }

  synchronized Set<Map.Entry<String, PexEntry>> getPexEntries() {
    return pexRecord.entrySet();
  }

  synchronized int getPexEntryCount() {
    return pexRecord.size();
  }

  synchronized String serialize() {
    return pexRecord.entrySet().stream()
        .map(entry -> entry.getKey() + ENTRY_SEP + entry.getValue().serialize())
        .collect(Collectors.joining("\n"));
  }

  static PexLedger deserialize(Collection<String> ledgerFile, int maxLedgerSize) {
    LOGGER.trace("Deserializing PEX");
    PexLedger pexLedger = new PexLedger(maxLedgerSize);

    for (String entry : ledgerFile) {
      try {
        String[] splitEntry = entry.split(ENTRY_SEP, 2);
        if (splitEntry.length == 2) {
          PexEntry pexEntry = PexEntry.deserialize(splitEntry[1]);
          pexLedger.addPexEntry(splitEntry[0], pexEntry);
        } else {
          LOGGER.warn("Pex entry must be of form 'userId,entry': {}", entry);
        }
      } catch (PexException e) {
        LOGGER.warn("Unable to parse PEX entry. {}", e.getMessage());
      }
    }
    return pexLedger;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PexLedger pexLedger = (PexLedger) o;

    if (maxLedgerSize != pexLedger.maxLedgerSize) return false;
    return pexRecord != null ? pexRecord.equals(pexLedger.pexRecord) : pexLedger.pexRecord == null;
  }
}
