package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by dominic on 13/04/17.
 */
public interface FileEventLogger {
  void protectShard(String shardId);

  void addFile(List<MD5HashLengthPair> shardHash, DateTime modificationDateTime, Path storedPath) throws StorageManagerException;

  void removeFile(Path storedPath, DateTime modificationDateTime) throws StorageManagerException;
}
