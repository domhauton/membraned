package com.domhauton.membrane;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger();
        logger.info("Starting Membrane");
        String configPath = args.length == 2 ? args[1] : getDefaultConfigLocation();
        new BackupManager(Paths.get(configPath));
    }

    private static String getDefaultConfigLocation() {
        return System.getProperty("user.home") + File.separator + ".config" + File.separator + "membrane.yaml";
    }

}
