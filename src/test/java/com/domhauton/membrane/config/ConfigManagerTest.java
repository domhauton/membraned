package com.domhauton.membrane.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by dominic on 23/01/17.
 */
class ConfigManagerTest {

    private ConfigManager configManager;
    private String testCfgLocation;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
        testCfgLocation = System.getProperty("java.io.tmpdir") +
                java.nio.file.FileSystems.getDefault().getSeparator() +
                "membrane-test-cfg.yaml";
    }

    @Test
    @DisplayName("Saving works")
    void saveConfig() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        configManager.saveConfig(testCfgLocation);
        Assertions.assertTrue(Files.deleteIfExists(Paths.get(testCfgLocation)));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));
    }

    @Test
    @DisplayName("Loading from a save")
    void saveLoadTest() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        configManager.saveConfig(testCfgLocation);
        // Should be empty
        Config config = configManager.loadConfig(testCfgLocation);
        Assertions.assertTrue(config.getFolders().isEmpty());
        Assertions.assertFalse(configManager.loadDefaultConfig().getFolders().isEmpty());
    }

    @Test
    @DisplayName("Loading from an overwritten save")
    void overwriteTest() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        configManager.saveConfig(testCfgLocation);
        Assertions.assertTrue(configManager.loadConfig(testCfgLocation).getFolders().isEmpty());
        Assertions.assertFalse(configManager.loadDefaultConfig().getFolders().isEmpty());
        configManager.saveConfig(testCfgLocation);
        Assertions.assertFalse(configManager.loadConfig(testCfgLocation).getFolders().isEmpty());
    }
}