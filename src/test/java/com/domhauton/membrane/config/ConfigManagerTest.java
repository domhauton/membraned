package com.domhauton.membrane.config;

import com.domhauton.membrane.config.items.WatchFolder;
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

        configManager.saveConfig(Paths.get(testCfgLocation), configManager.loadDefaultConfig());
        Assertions.assertTrue(Files.deleteIfExists(Paths.get(testCfgLocation)));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));
    }

    @Test
    @DisplayName("Loading from a save")
    void saveLoadTest() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        Config cfg =configManager.loadDefaultConfig();
        cfg.getFolders().add(new WatchFolder("foobar", false));

        configManager.saveConfig(Paths.get(testCfgLocation), cfg);
        Config config = configManager.loadConfig(Paths.get(testCfgLocation));
        Assertions.assertEquals(1, config.getFolders().size());
        Assertions.assertTrue(configManager.loadDefaultConfig().getFolders().isEmpty());
    }

    @Test
    @DisplayName("Loading from an overwritten save")
    void overwriteTest() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        Config cfg =configManager.loadDefaultConfig();
        cfg.getFolders().add(new WatchFolder("foobar", false));

        configManager.saveConfig(Paths.get(testCfgLocation), cfg);
        Assertions.assertFalse(configManager.loadConfig(Paths.get(testCfgLocation)).getFolders().isEmpty());
        Assertions.assertTrue(configManager.loadDefaultConfig().getFolders().isEmpty());
        cfg.getFolders().add(new WatchFolder("foobar2", false));
        configManager.saveConfig(Paths.get(testCfgLocation), cfg);
        Assertions.assertEquals(2, configManager.loadConfig(Paths.get(testCfgLocation)).getFolders().size());
    }
}