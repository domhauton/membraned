package com.domhauton.membrane.config;

import com.domhauton.membrane.config.items.WatchFolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
  @DisplayName("Saving IO Exception works")
  void saveConfigFails() throws Exception {
    Files.deleteIfExists(Paths.get(testCfgLocation));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

    ConfigManager.saveConfig(Paths.get(testCfgLocation), ConfigManager.loadDefaultConfig());
    Assertions.assertTrue(Paths.get(testCfgLocation).toFile().setWritable(false));
    Assertions.assertThrows(ConfigException.class, () -> ConfigManager.saveConfig(Paths.get(testCfgLocation), ConfigManager.loadDefaultConfig()));
    Assertions.assertTrue(Paths.get(testCfgLocation).toFile().setWritable(true));
    Assertions.assertTrue(Files.deleteIfExists(Paths.get(testCfgLocation)));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));
  }

  @Test
  @DisplayName("Loading IO Exception works")
  void loadConfigFails() throws Exception {
    Files.deleteIfExists(Paths.get(testCfgLocation));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

    ConfigManager.saveConfig(Paths.get(testCfgLocation), ConfigManager.loadDefaultConfig());
    Assertions.assertTrue(Paths.get(testCfgLocation).toFile().setReadable(false));
    Assertions.assertThrows(ConfigException.class, () -> ConfigManager.loadConfig(Paths.get(testCfgLocation)));
    Assertions.assertTrue(Paths.get(testCfgLocation).toFile().setReadable(true));

    Assertions.assertEquals(ConfigManager.loadDefaultConfig(), ConfigManager.loadConfig(Paths.get(testCfgLocation)));

    Assertions.assertTrue(Files.deleteIfExists(Paths.get(testCfgLocation)));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));
  }

  @Test
  @DisplayName("Loading invalid YAML works")
  void loadConfigFailsYAML() throws Exception {
    Files.deleteIfExists(Paths.get(testCfgLocation));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

    ConfigManager.saveConfig(Paths.get(testCfgLocation), ConfigManager.loadDefaultConfig());

    Files.write(Paths.get(testCfgLocation), "tsdatdra".getBytes(), StandardOpenOption.APPEND);

    Assertions.assertThrows(ConfigException.class, () -> ConfigManager.loadConfig(Paths.get(testCfgLocation)));

    Assertions.assertTrue(Files.deleteIfExists(Paths.get(testCfgLocation)));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));
  }

  @Test
  @DisplayName("Loading from a save")
  void saveLoadTest() throws Exception {
    Files.deleteIfExists(Paths.get(testCfgLocation));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

    Config cfg = ConfigManager.loadDefaultConfig();
    cfg.getWatcher().getFolders().add(new WatchFolder("foobar", false));

    ConfigManager.saveConfig(Paths.get(testCfgLocation), cfg);
    Config config = ConfigManager.loadConfig(Paths.get(testCfgLocation));
    Assertions.assertEquals(1, config.getWatcher().getFolders().size());
    Assertions.assertTrue(ConfigManager.loadDefaultConfig().getWatcher().getFolders().isEmpty());
  }

  @Test
  @DisplayName("Loading from an overwritten save")
  void overwriteTest() throws Exception {
    Files.deleteIfExists(Paths.get(testCfgLocation));
    Assertions.assertFalse(Files.deleteIfExists(Paths.get(testCfgLocation)));

    Config cfg = ConfigManager.loadDefaultConfig();
    cfg.getWatcher().getFolders().add(new WatchFolder("foobar", false));

    ConfigManager.saveConfig(Paths.get(testCfgLocation), cfg);
    Assertions.assertFalse(ConfigManager.loadConfig(Paths.get(testCfgLocation)).getWatcher().getFolders().isEmpty());
    Assertions.assertTrue(ConfigManager.loadDefaultConfig().getWatcher().getFolders().isEmpty());
    cfg.getWatcher().getFolders().add(new WatchFolder("foobar2", false));
    ConfigManager.saveConfig(Paths.get(testCfgLocation), cfg);
    Assertions.assertEquals(2, ConfigManager.loadConfig(Paths.get(testCfgLocation)).getWatcher().getFolders().size());
  }
}