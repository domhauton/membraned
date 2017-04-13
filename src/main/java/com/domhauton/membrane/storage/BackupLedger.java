package com.domhauton.membrane.storage;

import java.util.List;
import java.util.Set;

/**
 * Created by dominic on 13/04/17.
 */
public interface BackupLedger {
  Set<String> getAllRequiredShards();

  List<String> getAllRelatedJournalEntries(String shardId);

  void insertJournalEntry(String serializedEntry) throws StorageManagerException;
}
