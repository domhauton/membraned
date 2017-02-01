package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dominic on 30/01/17.
 */
public class JournalEntry {

    private final DateTime dateTime;
    private final FileVersion shardInfo;
    private final FileOperation fileOperation;
    private final Path filePath;

    public JournalEntry(DateTime dateTime, FileVersion shardInfo, FileOperation fileOperation, Path filePath) {
        this.dateTime = dateTime;
        this.shardInfo = shardInfo;
        this.fileOperation = fileOperation;
        this.filePath = filePath;
    }

    public JournalEntry(String string) throws IllegalArgumentException {
        List<String> decoded = CatalogueUtils.stringToList(string);
        if(decoded.size() >= 4) {
            try {
                dateTime = new DateTime(Long.parseLong(decoded.get(0)));
                fileOperation = FileOperation.valueOf(decoded.get(1));
                filePath = Paths.get(decoded.get(2));
                DateTime modifiedDateTime = new DateTime(Long.parseLong(decoded.get(3)));
                List<String> hashes = decoded.subList(4, decoded.size());
                shardInfo = new FileVersion(hashes, modifiedDateTime);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException("Not enough arguments: (" + decoded.size() + ") " + string);
        }
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public FileVersion getShardInfo() {
        return shardInfo;
    }

    public FileOperation getFileOperation() {
        return fileOperation;
    }

    public Path getFilePath() {
        return filePath;
    }

    public static Comparator<JournalEntry> getComparator() {
        return Comparator.comparing(JournalEntry::getDateTime);
    }

    @Override
    public String toString() {
        List<String> baseList = Arrays.asList(Long.toString(dateTime.getMillis()),
                fileOperation.toString(),
                filePath.toString(),
                Long.toString(shardInfo.getModificationDateTime().getMillis()));
        LinkedList<String> retList = new LinkedList<>();
        retList.addAll(baseList);
        retList.addAll(shardInfo.getMD5ShardList());
        return CatalogueUtils.listToString(retList);
    }


}
