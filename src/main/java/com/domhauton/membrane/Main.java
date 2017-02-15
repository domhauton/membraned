package com.domhauton.membrane;


import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Main {
  public static void main(String[] args) {
    Logger logger = LogManager.getLogger();
    logger.info("Starting Membrane");

    String arg;
    char flag;
    int verboseLogging = 0;
    boolean demoMode = false;
    Optional<String> configFile = Optional.empty();

    for (int i = 0; i < args.length && args[i].startsWith("-"); i++) {
      arg = args[i];

      switch (arg) {
        case "-verbose":
          verboseLogging++;
          break;
        case "-config":
        case "-c":
          i++;
          if (i < args.length)
            configFile = Optional.of(args[i++]);
          else
            logger.error("-config requires a filename");
          break;
        default:
          for (int j = 1; j < arg.length(); j++) {
            flag = arg.charAt(j);
            switch (flag) {
              case 'm':
                demoMode = true;
                logger.info("Monitor-only mode enabled. Will not write to storage.");
                break;
              case 'v':
                verboseLogging++;
                break;
              case 'c':
                logger.error("-c requires a filename");
                System.exit(0);
                break;
              default:
                logger.error("Illegal commandline option " + flag);
                System.exit(0);
                break;
            }
          }
          break;
      }
    }

    if (verboseLogging > 0) {
      LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
      Configuration config = ctx.getConfiguration();
      LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
      if (verboseLogging == 1 && !logger.isDebugEnabled()) {
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();
      } else if (verboseLogging >= 2 && !logger.isTraceEnabled()) {
        loggerConfig.setLevel(Level.TRACE);
        ctx.updateLoggers();
      }
    }

    Path configPath = configFile.isPresent() ? Paths.get(configFile.get()) : getDefaultConfigLocation();
    logger.info("Using config [{}]", configPath);
    logger.debug("Verbose logging enabled.");
    logger.debug("Trace logging enabled.");

    try {
      Config config = configPath.toFile().exists() ? ConfigManager.loadConfig(configPath) : ConfigManager.loadDefaultConfig();
      BackupManager backupManager = new BackupManager(config, configPath, demoMode);
      backupManager.registerShutdownHook();
      backupManager.start();
    } catch (ConfigException e) {
      logger.fatal("Unable to load config [{}]. Refusing to start up.", configPath);
    } catch (IllegalArgumentException e) {
      logger.fatal("Failed to startup with given config. {}", e.toString());
    } catch (Exception e) {
      logger.fatal("An unknown error occurred. {}", e);
    }
  }

  private static Path getDefaultConfigLocation() {
    LogManager.getLogger().info("Loading default config.");
    return Paths.get(System.getProperty("user.home") + File.separator + ".config" + File.separator + "membrane.yaml");
  }

}
