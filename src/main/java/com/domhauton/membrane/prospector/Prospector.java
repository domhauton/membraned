package com.domhauton.membrane.prospector;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by dominic on 23/01/17.
 *
 * Scans folders searching for files.
 */
public class Prospector {

    private Logger logger;
    private Path path;
    private Map<String, DateTime> fileState;

    public Prospector(String filePath) {
        this.path = Paths.get(filePath);
        logger = org.apache.logging.log4j.LogManager.getLogger();
        fileState = new HashMap<>();
        logger.info("Prospector started for [{}]", filePath);
    }

    public Set<String> modifiedFiles() {
        Map<String, DateTime> oldFileState = fileState;
        fileState = getFolderState();

        Set<String> addedFiles = fileState
                .keySet()
                .stream()
                .filter(file -> !oldFileState.containsKey(file))
                .peek(s -> logger.debug("File addition detected [{}]", s))
                .collect(Collectors.toSet());

        Set<String> updatedFiles = fileState.entrySet()
                .stream()
                .filter(entry -> oldFileState.containsKey(entry.getKey()))
                .filter(entry -> oldFileState.getOrDefault(entry.getKey(), new DateTime(0L))
                                .isBefore(entry.getValue()))
                .map(Map.Entry::getKey)
                .peek(s -> logger.debug("File change detected [{}]", s))
                .collect(Collectors.toSet());

        Set<String> removedFiles = oldFileState
                .keySet()
                .stream()
                .filter(file -> !fileState.containsKey(file))
                .peek(s -> logger.debug("File remove detected [{}]", s))
                .collect(Collectors.toSet());

        updatedFiles.addAll(removedFiles);
        updatedFiles.addAll(addedFiles);
        return updatedFiles;
    }

    Map<String, DateTime> getFolderState() {
        Map<String, DateTime> retMap = new HashMap<>();
        getFiles().forEach(s -> retMap.put(s, lastModifiedTime(s)));
        return retMap;
    }

    DateTime lastModifiedTime(String file) {
        Path p = Paths.get(file);
        BasicFileAttributeView view = Files.getFileAttributeView(p, BasicFileAttributeView.class);
        try {
            BasicFileAttributes attributes = view.readAttributes();
            FileTime fileTime = attributes.lastModifiedTime();
            return new DateTime(fileTime.toMillis());
        } catch (IOException e) {
            return DateTime.now(); // If can't fetch time assume modification.
        }
    }

    List<String> getFiles() {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            return StreamSupport.stream(directoryStream.spliterator(), false)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }
}
