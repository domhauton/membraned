package com.domhauton.membrane;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger();
        logger.info("Starting Membrane");
        new BackupManager();
    }
}
