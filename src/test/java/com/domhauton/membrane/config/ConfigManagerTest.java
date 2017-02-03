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
        testCfgLocation = System.getProperty("java.io.tmpdir") +
                java.nio.file.FileSystems.getDefault().getSeparator() +
                "membrane-test-cfg.yaml";
    }

    @Test
    @DisplayName("Saving works")
    void saveConfig() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        ConfigManager.saveConfig(Paths.get(testCfgLocation), ConfigManager.loadDefaultConfig());
        Assertions.assertTrue(Files.deleteIfExists(Paths.get(testCfgLocation)));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));
    }

    @Test
    @DisplayName("Loading from a save")
    void saveLoadTest() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        Config cfg = ConfigManager.loadDefaultConfig();
        cfg.getFolders().add(new WatchFolder("foobar", false));

        ConfigManager.saveConfig(Paths.get(testCfgLocation), cfg);
        Config config = ConfigManager.loadConfig(Paths.get(testCfgLocation));
        Assertions.assertEquals(1, config.getFolders().size());
        Assertions.assertTrue(ConfigManager.loadDefaultConfig().getFolders().isEmpty());
    }

    @Test
    @DisplayName("Loading from an overwritten save")
    void overwriteTest() throws Exception {
        Files.deleteIfExists(Paths.get(testCfgLocation));
        Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

        Config cfg = ConfigManager.loadDefaultConfig();
        cfg.getFolders().add(new WatchFolder("foobar", false));

        ConfigManager.saveConfig(Paths.get(testCfgLocation), cfg);
        Assertions.assertFalse(ConfigManager.loadConfig(Paths.get(testCfgLocation)).getFolders().isEmpty());
        Assertions.assertTrue(ConfigManager.loadDefaultConfig().getFolders().isEmpty());
        cfg.getFolders().add(new WatchFolder("foobar2", false));
        ConfigManager.saveConfig(Paths.get(testCfgLocation), cfg);
        Assertions.assertEquals(2, ConfigManager.loadConfig(Paths.get(testCfgLocation)).getFolders().size());
    }
}