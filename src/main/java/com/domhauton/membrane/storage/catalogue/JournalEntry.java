package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                List<MD5HashLengthPair> MD5HashLengthPairs = new LinkedList<>();
                List<String> dataSubList = decoded.subList(4, decoded.size());
                if(dataSubList.size() % 2 != 0) {
                    throw new IllegalArgumentException("Entry has incomplete shard information");
                }
                for(int i = 0; i < dataSubList.size(); i++) {
                    String md5Hash = dataSubList.get(i++);
                    Integer length = Integer.parseInt(dataSubList.get(i));
                    MD5HashLengthPairs.add(new MD5HashLengthPair(md5Hash, length));
                }
                shardInfo = new FileVersion(MD5HashLengthPairs, modifiedDateTime);
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
        retList.addAll(shardInfo.getMD5HashLengthPairs().stream()
                .flatMap(x -> Stream.of(x.getMd5Hash(), x.getLength().toString()))
                .collect(Collectors.toList()));
        return CatalogueUtils.listToString(retList);
    }


}
