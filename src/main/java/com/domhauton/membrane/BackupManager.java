package com.domhauton.membrane;

import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.prospector.FileManager;
import com.domhauton.membrane.prospector.FileManagerException;
import com.domhauton.membrane.storage.StorageManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by dominic on 23/01/17.
 */
public class BackupManager {
    private ConfigManager configManager;
    private FileManager fileManager;
    private StorageManager storageManager;
    private Logger logger;

    public BackupManager() {
        logger = LogManager.getLogger();
        configManager = new ConfigManager();
        try {
            Config config = configManager.loadConfig();
            fileManager = new FileManager();
            storageManager = new StorageManager();
            config.getFolders().forEach(fileManager::addWatchFolder);
        } catch (ConfigException | FileManagerException e) {
           e.printStackTrace();
        }
    }
}
