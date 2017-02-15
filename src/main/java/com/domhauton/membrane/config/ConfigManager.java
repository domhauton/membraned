package com.domhauton.membrane.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by dominic on 23/01/17.
 * <p>
 * Responsible for loading and saving the config
 */
public abstract class ConfigManager {
  private final static Logger logger = LogManager.getLogger();
  private final static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  public static void saveConfig(Path configPath, Config config) throws ConfigException {
    try {
      logger.info("Membrane Config - Saving config. \t\t[{}]", configPath);
      mapper.writeValue(configPath.toFile(), config);
    } catch (IOException e) {
      logger.error("Membrane Config - Failed to open file. \t[{}]", configPath);
      logger.debug(e);
      throw new ConfigException("Failed to open file. " + e.getMessage());
    }
  }

  public static Config loadConfig(Path filePath) throws ConfigException {
    try {
      logger.info("Membrane Config - Loading config. \t[{}]", filePath);
      return mapper.readValue(filePath.toFile(), Config.class);
    } catch (JsonParseException e) {
      logger.error("Membrane Config - Failed to parse YAML. \t[{}]", filePath);
      logger.debug(e);
      throw new ConfigException("Failed to parse YAML. " + e.getMessage());
    } catch (JsonMappingException e) {
      logger.error("Membrane Config - Config could not be parsed. \t[{}]", filePath);
      logger.debug(e);
      throw new ConfigException("Config has invalid fields. " + e.getMessage());
    } catch (IOException e) {
      logger.error("Membrane Config - Failed to open file. \t[{}]", filePath);
      logger.debug(e);
      throw new ConfigException("Failed to open file. " + e.getMessage());
    }
  }

  public static Config loadDefaultConfig() throws ConfigException {
    try {
      return new Config();
    } catch (NullPointerException e) {
      logger.error("Membrane Config - Could not find default config!");
      throw new ConfigException("Failed to locate default config.");
    }
  }
}
