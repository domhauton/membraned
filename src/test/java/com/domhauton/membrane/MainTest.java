package com.domhauton.membrane;

import com.domhauton.membrane.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Dominic Hauton on 25/02/17.
 */
class MainTest {
  @Test
  void testBasicStartupSixSecondRun() throws Exception {
    Main.start(new String[]{"-vvm"});
    Thread.sleep(6000);
    Main.backupManager.close();
  }

  @Test
  void testBasicStartupBadCustomConfig() throws Exception {
    Path fakeConfig = Paths.get("/tmp/membrane/fakeconfig");
    Files.createDirectories(fakeConfig.getParent());
    Files.createFile(fakeConfig);
    Assertions.assertThrows(ConfigException.class, () -> Main.start(new String[]{"--config", fakeConfig.toString()}));
    Files.deleteIfExists(fakeConfig);
  }

  @Test
  void testBasicStartupBadConfigArgs() throws Exception {
    Assertions.assertThrows(IllegalArgumentException.class, () -> Main.start(new String[]{"-cm"}));
  }

  @Test
  void testBasicStartupBigVerboseFlag() throws Exception {
    Main.start(new String[]{"--verbose", "--verbose", "-vv"});
    Thread.sleep(1000);
    Main.backupManager.close();
  }

  @Test
  void testBasicStartupBadArgs() throws Exception {
    Assertions.assertThrows(IllegalArgumentException.class, () -> Main.start(new String[]{"-x"}));
  }

  @Test
  void testBasicTrackerFiveSecondRun() throws Exception {
    Main.start(new String[]{"-vm", "--tracker"});
    Thread.sleep(5000);
    Main.networkManager.close();
  }
}