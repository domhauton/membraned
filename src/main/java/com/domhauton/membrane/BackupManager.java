package com.domhauton.membrane;

import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.prospector.FileManager;
import com.domhauton.membrane.prospector.FileManagerException;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Map;

/**
 * Created by dominic on 23/01/17.
 */
public class BackupManager {
    private ConfigManager configManager;
    private Config config;
    private FileManager fileManager;
    private StorageManager storageManager;
    private Logger logger;

    public BackupManager() {
        logger = LogManager.getLogger();
        configManager = new ConfigManager();
        try {
            config = configManager.loadConfig();
            fileManager = new FileManager();
            storageManager = new StorageManager();
        } catch (ConfigException | FileManagerException | StorageManagerException e) {
            logger.error("Failed to start membrane backup manager.");
            logger.error(e.getMessage());
            System.exit(0);
        }
    }

    public void start() {
        Map<Path, FileVersion> currentFileMapping = storageManager.getCurrentFileMapping();
        currentFileMapping.entrySet()
                .forEach(x -> fileManager.addExistingFile(x.getKey(), x.getValue().getModificationDateTime(),
                        x.getValue().getMD5ShardList()));
        config.getFolders().forEach(fileManager::addWatchFolder);

    }
}
