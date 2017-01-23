package com.domhauton.membrane.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by dominic on 23/01/17.
 *
 * Responsible for loading and saving the config
 */
public class ConfigManager {
    private Config config;
    private String saveDestination;
    private Logger logger;
    private ObjectMapper mapper;

    public ConfigManager() {
        logger = LogManager.getLogger();
        config = new Config();
        saveDestination = getDefaultExternalLocation();
        mapper = new ObjectMapper(new YAMLFactory());
    }

    void saveConfig(String fileName) throws ConfigException {
        try {
            logger.info("Saving config. \t\t[{}]", fileName);
            mapper.writeValue(new File(fileName), config);
        } catch (IOException e) {
            logger.error("Failed to open file. \t[{}]", fileName);
            logger.debug(e);
            throw new ConfigException("Failed to open file. " + e.getMessage());
        }
    }

    public void saveConfig() throws ConfigException {
        saveConfig(saveDestination);
    }

    Config loadConfig(String fileName) throws ConfigException {
        try{
            logger.info("Loading config. \t[{}]", fileName);
            config = mapper.readValue(new File(fileName), Config.class);
            saveDestination = fileName;
            return config;
        } catch (JsonParseException e) {
            logger.error("Failed to parse YAML. \t[{}]", fileName);
            logger.debug(e);
            throw new ConfigException("Failed to parse YAML. " + e.getMessage());
        } catch (JsonMappingException e) {
            logger.error("Config could not be parsed. \t[{}]", fileName);
            logger.debug(e);
            throw new ConfigException("Config has invalid fields. " + e.getMessage());
        } catch (IOException e) {
            logger.error("Failed to open file. \t[{}]", fileName);
            logger.debug(e);
            throw new ConfigException("Failed to open file. " + e.getMessage());
        }
    }

    Config loadDefaultConfig() throws ConfigException {
        URL url = getClass().getClassLoader().getResource("membrane.yaml");
        try {
            String path = url != null ? url.getPath() : null;
            Config config = loadConfig(path);
            // Do not save to default config.
            saveDestination = getDefaultExternalLocation();
            return config;
        } catch (NullPointerException e) {
            logger.error("Could not find default config!");
            throw new ConfigException("Failed to locate default config.");
        }
    }

    public Config loadConfig() throws ConfigException {
        try {
            return loadConfig(getDefaultExternalLocation());
        } catch (ConfigException e) {
            return loadDefaultConfig();
        }
    }

    String getDefaultExternalLocation() {
        String home = System.getProperty("user.home");
        String separator = java.nio.file.FileSystems.getDefault().getSeparator();
        return home + separator + ".config" + separator + "membrane.yaml";
    }
}
