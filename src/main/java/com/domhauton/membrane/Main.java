package com.domhauton.membrane;


import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.network.NetworkException;
import com.domhauton.membrane.network.NetworkManager;
import com.domhauton.membrane.network.NetworkManagerImpl;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

class Main {
  private static final Logger LOGGER = LogManager.getLogger();

  static BackupManager backupManager;
  static NetworkManager networkManager;

  public static void main(String[] args) {
    LOGGER.info("Starting Membrane");
    try {
      start(args);
    } catch (Exception e) {
      System.exit(1);
    }
  }

  static void start(String[] args) throws IllegalArgumentException, ConfigException, NetworkException {
    String arg;
    char flag;
    int verboseLogging = 0;
    boolean demoMode = false;
    boolean trackerMode = false;
    Optional<String> configFile = Optional.empty();

    for (int i = 0; i < args.length && args[i].startsWith("-"); i++) {
      arg = args[i];

      switch (arg) {
        case "--verbose":
          verboseLogging++;
          break;
        case "--config":
        case "-c":
          i++;
          if (i < args.length) {
            configFile = Optional.of(args[i++]);
          } else {
            LOGGER.error("-config requires a filename");
          }
          break;
        case "--tracker":
          trackerMode = true;
          break;
        default:
          for (int j = 1; j < arg.length(); j++) {
            flag = arg.charAt(j);
            switch (flag) {
              case 'm':
                demoMode = true;
                LOGGER.info("Monitor-only mode enabled. Will not write to storage.");
                break;
              case 'v':
                verboseLogging++;
                break;
              case 'c':
                LOGGER.error("-c requires a filename");
                throw new IllegalArgumentException("-c requires a filename");
              default:
                LOGGER.error("Illegal commandline option " + flag);
                throw new IllegalArgumentException("Illegal commandline option " + flag);
            }
          }
          break;
      }
    }

    if (verboseLogging > 0) {
      LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
      Configuration config = ctx.getConfiguration();
      LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
      if (verboseLogging == 1 && !LOGGER.isDebugEnabled()) {
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();
      } else if (verboseLogging >= 2 && !LOGGER.isTraceEnabled()) {
        loggerConfig.setLevel(Level.TRACE);
        ctx.updateLoggers();
      }
    }

    Path configPath = configFile.isPresent() ? Paths.get(configFile.get()) : getDefaultConfigLocation();
    LOGGER.info("Using config [{}]", configPath);
    LOGGER.debug("Verbose logging enabled.");
    LOGGER.trace("Trace logging enabled.");

    try {
      if (trackerMode) {

        Path trackerPath = Paths.get(configPath.getParent().toString() + File.separator + "tracker");
        LOGGER.info("Running tracker mode with base path [{}]", trackerPath);
        int transportPort = 14200;
        networkManager = new NetworkManagerImpl(trackerPath, transportPort, -1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            networkManager.close();
          } catch (IOException e) {
            LOGGER.error("Error shutting down network manager.");
          }
        }));

        networkManager.run();
        LOGGER.info("Running network manager at {} with uid [{}]", transportPort, networkManager.getUID());
        System.out.println("Running network manager at " + transportPort + " with uid [" + networkManager.getUID() + "]");
      } else {
        Config config = configPath.toFile().exists() ? ConfigManager.loadConfig(configPath) : ConfigManager.loadDefaultConfig();
        backupManager = new BackupManager(config, configPath, demoMode);
        backupManager.registerShutdownHook();
        backupManager.run();
      }
    } catch (ConfigException e) {
      LOGGER.fatal("Unable to load config [{}]. Refusing to run up.", configPath);
      throw e;
    } catch (IllegalArgumentException e) {
      LOGGER.fatal("Failed to startup with given config. {}", e.toString());
      throw e;
    } catch (NetworkException e) {
      LOGGER.fatal("Failed to startup network manager. {}", e.toString());
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.fatal("An unknown error occurred. {}", e.getMessage());
      throw e;
    }
  }

  private static Path getDefaultConfigLocation() {
    LogManager.getLogger().info("Loading default config.");
    return Paths.get(System.getProperty("user.home")
        + File.separator + ".config"
        + File.separator + "membrane"
        + File.separator + "membrane.yaml");
  }

}
