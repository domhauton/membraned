package com.domhauton.membrane.prospector;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import com.domhauton.membrane.config.items.WatchFolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by dominic on 26/01/17.
 */
public class FileManager {
    private Logger logger;
    private Prospector prospector;
    private Map<Path, FileMetadata> managedFiles;

    public FileManager() throws FileManagerException {
        logger = LogManager.getLogger();
        try {
            prospector = new Prospector();
        } catch (IOException e) {
            logger.error("Unable to start Prospector.");
            throw new FileManagerException("Failed to start prospector due to IO exception on watcher.");
        }
        this.managedFiles = new HashMap<>();
    }

    private void folderRescan() {
        prospector.checkChanges();
    }

    public void addWatchfolder(WatchFolder watchFolder) {
        Set<Path> newPaths = prospector.addFolder(watchFolder);
        addExistingFiles(newPaths);
    }

    public void addExistingFiles(Collection<Path> folders) {
        folders.stream()
                .map(Path::toFile)
                .map(File::listFiles)
                .flatMap(Arrays::stream)
                .map(File::toPath)
                .distinct()
                .forEach(this::addFile);
    }

    private void addFile(Path path) {
        ByteSource byteSource = Files.asByteSource(path.toFile());
        try {
            byte[] bytes = byteSource.read();
            FileMetadata fileMetadata = new FileMetadata(path.toString(), bytes);
            FileMetadata oldMetaData = managedFiles.getOrDefault(path, null);
            if (!fileMetadata.hashCodeEqual(oldMetaData)) {
                //TODO Send byte update notification to updater;
                managedFiles.put(path, fileMetadata);
            }
        } catch (IOException e) {
            logger.error("Unable to read and update [{}]", path);
        }
    }
}
