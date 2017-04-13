package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by dominic on 13/04/17.
 */
public class FileEventLoggerImpl implements FileEventLogger {
  @Override
  public void protectShard(String shardId) {
    // Do Nothing
  }

  @Override
  public void addFile(List<MD5HashLengthPair> shardHash, DateTime modificationDateTime, Path storedPath) throws StorageManagerException {
    throw new StorageManagerException("Temporary file event logger. Cannot Store.");
  }

  @Override
  public void removeFile(Path storedPath, DateTime modificationDateTime) throws StorageManagerException {
    throw new StorageManagerException("Temporary file event logger. Cannot remove file.");
  }
}
