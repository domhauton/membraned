package com.domhauton.membrane;

import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by dominic on 23/01/17.
 */
public class BackupManager {
    private ConfigManager configManager;
    private Logger logger;

    public BackupManager() {
        logger = LogManager.getLogger();
        configManager = new ConfigManager();
        try {
            configManager.loadConfig();
            configManager.saveConfig();
        } catch (ConfigException e) {
           // e.printStackTrace();
        }
    }
}
