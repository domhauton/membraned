package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.journal.StorageJournal;
import com.domhauton.membrane.storage.metadata.FileOperation;
import com.domhauton.membrane.storage.metadata.FileVersion;
import com.domhauton.membrane.storage.shard.ShardStorageImpl;
import org.joda.time.DateTime;
import ru.serce.jnrfuse.NotImplemented;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dominic on 30/01/17.
 */
public class FileCatalogue {
    Map<Path, FileVersion> fileInfoMap;
    StorageJournal storageJournal;
    ShardStorageImpl shardStorage;

    public FileCatalogue() {
        fileInfoMap = new HashMap<>();
        storageJournal = new StorageJournal();
    }

    public void addFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath) {
        FileVersion fileVersion = new FileVersion(shardHash, modificationDateTime, storedPath);
        storageJournal.addEntry(fileVersion, FileOperation.ADD, storedPath);
        fileInfoMap.put(storedPath, fileVersion);
    }

    public void removeFile(Path storedPath, DateTime modificationDateTime) {
        FileVersion fileVersion = new FileVersion(Collections.emptyList(), modificationDateTime, storedPath);
        storageJournal.addEntry(fileVersion, FileOperation.ADD, storedPath);
        fileInfoMap.remove(storedPath);
    }

    public void pruneJournal() {
        //FIXME: Not implemented yet!
    }
}
