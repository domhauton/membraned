package com.domhauton.membrane;


import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger();
        logger.info("Starting Membrane");
        Path configPath = args.length == 2 ? Paths.get(args[1]) : getDefaultConfigLocation();
        logger.info("Using config [{}]", configPath);
        try {
            Config config = configPath.toFile().exists() ? ConfigManager.loadConfig(configPath) : ConfigManager.loadDefaultConfig();
            BackupManager backupManager = new BackupManager(config, configPath);
            backupManager.registerShutdownHook();
            backupManager.start();
        } catch (ConfigException e) {
            logger.error("Unable to load config [{}]. Refusing to start up.", configPath);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to startup with given config. {}", e.toString());
        }
    }

    private static Path getDefaultConfigLocation() {
        LogManager.getLogger().info("Loading default config.");
        return Paths.get(System.getProperty("user.home") + File.separator + ".config" + File.separator + "membrane.yaml");
    }

}
