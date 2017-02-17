package com.domhauton.membrane.restful;

import com.domhauton.membrane.BackupManager;
import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.restful.requests.WatchFolderChange;
import com.domhauton.membrane.restful.responses.*;
import com.domhauton.membrane.restful.responses.config.RestAPIConfig;
import com.domhauton.membrane.restful.responses.config.StorageConfig;
import com.domhauton.membrane.restful.responses.config.WatchFoldersInfo;
import com.domhauton.membrane.restful.responses.config.WatcherConfig;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 03/02/17.
 */
public class RestfulAPI {
  private static final long BODY_LIMIT = 1024 * 1024 * 64;
  private static String ALLOWED_HOST = "127.0.0.1";

  private final Logger logger;
  private final int port;
  private final Vertx vertx;
  private final HttpServer httpServer;
  private final Router router;
  private final ObjectMapper objectMapper;

  private final BackupManager backupManager;

  public RestfulAPI(int port, BackupManager backupManager) {
    logger = LogManager.getLogger();
    this.port = port;
    this.backupManager = backupManager;
    vertx = Vertx.vertx();
    httpServer = vertx.createHttpServer();
    router = Router.router(vertx);
    router.route().handler(BodyHandler.create().setBodyLimit(BODY_LIMIT));
    objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY);
  }

  public void start() {
    router.route().handler(this::hostFilter);
    router.get("/").handler(this::rootHandler);
    router.get("/status/config").handler(this::getMembraneConfig);
    router.get("/status/watcher").handler(this::getFileWatcherStatus);
    router.get("/status/storage").handler(this::getStorageStatus);

    router.post("/configure/config").blockingHandler(this::putNewConfig);
    router.post("/configure/watch_folder").blockingHandler(this::modifyWatchFolder);

    router.post("/request/cleanup").blockingHandler(this::putRequestCleanup);

    router.get("/request/history").blockingHandler(this::getFileHistory);
    router.post("/request/reconstruct").blockingHandler(this::reconstructFile);

    httpServer.requestHandler(router::accept).listen(port);
    logger.info("Listening at localhost:{}", port);
  }

  public void close() {
    router.clear();
    httpServer.close();
  }

  private void hostFilter(RoutingContext routingContext) {
    String hostIP = routingContext.request().remoteAddress().host();
    if (hostIP.equals(ALLOWED_HOST)) {
      routingContext.next();
    } else {
      logger.warn("Connection from {} blocked.", hostIP);
      routingContext.response()
              .putHeader("content-type", "text/plain")
              .setStatusCode(403)
              .end("Access forbidden. Must be " + ALLOWED_HOST);
    }
  }

  private void rootHandler(RoutingContext routingContext) {
    MembraneStatus membraneStatus = backupManager.isMonitorMode() ?
            MembraneStatus.MONITOR_MODE : MembraneStatus.NORMAL;
    String version = backupManager.getVersion();
    DateTime startTime = backupManager.getStartTime();

    MembraneInfo runtimeInfo = new MembraneInfo(port, startTime, version, membraneStatus, "Welcome to Membrane!");
    try {
      String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(runtimeInfo);
      logger.info("Sending response to {}", routingContext.request().remoteAddress().host());
      routingContext.response()
              .putHeader("content-type", "application/json")
              .setStatusCode(200)
              .end(response);
    } catch (JsonProcessingException e) {
      logger.error("Could not generate JSON ping-pong response. {}", e.getMessage());
      routingContext.response()
              .putHeader("content-type", "application/json")
              .setStatusCode(500)
              .end("Unable to generate response. Please check the logs.\n" + e.getMessage());
    }
  }

  private void getMembraneConfig(RoutingContext routingContext) {
    Config config = backupManager.getConfig();
    RestAPIConfig restAPIConfig = new RestAPIConfig(config.getVerticlePort());
    StorageConfig storageConfig = new StorageConfig(
            config.getGarbageCollectThresholdMB(),
            config.getMaxStorageSizeMB(),
            config.getShardStorageFolder(),
            config.getStorageTrimFrequencyMin());
    List<WatchFoldersInfo> watchFoldersInfoList = config.getFolders().stream()
            .map(x -> new WatchFoldersInfo(x.getDirectory(), x.getRecursive()))
            .collect(Collectors.toList());
    WatcherConfig watcherConfig = new WatcherConfig(
            config.getFileRescanFrequencySec(),
            config.getFolderRescanFrequencySec(),
            config.getChunkSizeMB(),
            watchFoldersInfoList);
    MembraneRestConfig membraneRestConfig = new MembraneRestConfig(watcherConfig, storageConfig, restAPIConfig);
    logger.info("Sending config status to {}", routingContext.request().remoteAddress().host());
    sendObject(routingContext, membraneRestConfig);
  }

  private void getFileWatcherStatus(RoutingContext routingContext) {
    Set<String> watchedFolders = backupManager.getWatchedFolders();
    Set<String> watchedFiles = backupManager.getWatchedFiles();
    FileManagerStatus fileManagerStatus = new FileManagerStatus(watchedFolders, watchedFiles);
    logger.info("Sending file watcher status to {}", routingContext.request().remoteAddress().host());
    sendObject(routingContext, fileManagerStatus);
  }

  private void getStorageStatus(RoutingContext routingContext) {
    Set<Path> currentFiles = backupManager.getCurrentFiles();
    Set<Path> referencedFiles = backupManager.getReferencedFiles();
    long currentStorageSize = backupManager.getStorageSize();
    StorageManagerStatus storageManagerStatus = new StorageManagerStatus(currentFiles, referencedFiles, currentStorageSize);
    logger.info("Sending storage status to {}", routingContext.request().remoteAddress().host());
    sendObject(routingContext, storageManagerStatus);
  }

  private void sendObject(RoutingContext routingContext, MembraneResponse membraneResponse) {
    try {
      String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(membraneResponse);
      routingContext.response()
              .putHeader("content-type", "application/json")
              .setStatusCode(200)
              .end(response);
    } catch (JsonProcessingException e) {
      logger.error("Could not generate response. {}", e.getMessage());
      routingContext.response()
              .putHeader("content-type", "application/json")
              .setStatusCode(500)
              .end("Unable to generate response. Please check the logs.\n" + e.getMessage());
    }
  }

  private void putNewConfig(RoutingContext routingContext) {
    final MembraneRestConfig membraneRestConfig = Json.decodeValue(routingContext.getBodyAsString(), MembraneRestConfig.class);
    Config config = new Config(
            membraneRestConfig.getStorageConfig().getDirectory(),
            membraneRestConfig.getWatcherConfig().getFileRescan(),
            membraneRestConfig.getWatcherConfig().getFolderRescan(),
            membraneRestConfig.getStorageConfig().getTrimFrequency(),
            membraneRestConfig.getWatcherConfig().getChunkSize(),
            membraneRestConfig.getStorageConfig().getSoftStorageCap(),
            membraneRestConfig.getStorageConfig().getHardStorageCap(),
            membraneRestConfig.getWatcherConfig().getWatchFolders().stream().map(x -> new WatchFolder(x.getDirectory(), x.isRecursive())).collect(Collectors.toList()),
            membraneRestConfig.getRestAPIConfig().getPort());
    Path configPath = backupManager.getConfigPath();
    try {
      ConfigManager.saveConfig(configPath, config);
      logger.info("Successfully wrote new config.");
      routingContext.response().setStatusCode(200).end("Successfully wrote config to: " + configPath.toString());
    } catch (ConfigException e) {
      logger.warn("Failed to write new config. {}", e.getMessage());
      routingContext.response().setStatusCode(500).end("Failed to write config to: " + configPath.toString() + "Error: " + e.getMessage());
    }
  }

  private void modifyWatchFolder(RoutingContext routingContext) {
    final WatchFolderChange watchFolderChange = Json.decodeValue(routingContext.getBodyAsString(), WatchFolderChange.class);
    Path configPath = backupManager.getConfigPath();
    try {
      if (watchFolderChange.getType() == WatchFolderChange.Type.ADD) {
        backupManager.addWatchFolder(watchFolderChange.getWatchFolder());
        logger.info("Successfully added watch folder.");
        routingContext.response().setStatusCode(200).end("Added and persisted successfully.");
      } else if (watchFolderChange.getType() == WatchFolderChange.Type.REMOVE) {
        backupManager.removeWatchFolder(watchFolderChange.getWatchFolder());
        logger.info("Successfully removed watch folder.");
        routingContext.response().setStatusCode(200).end("Removed and persisted successfully.");
      } else {
        logger.warn("Received invalid modify watch folder request: {}", routingContext.getBodyAsString());
        routingContext.response().setStatusCode(400).end("Invalid request.");
      }
    } catch (ConfigException e) {
      routingContext.response().setStatusCode(304).end("Chance complete but failed to persist to: " + configPath.toString() + "Error: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      routingContext.response().setStatusCode(400).end("Could not perform. Error: " + e.getMessage());
    }
  }

  private void putRequestCleanup(RoutingContext routingContext) {
    try {
      backupManager.trimStorageAttempt();
      routingContext.response().setStatusCode(200).end("Successfully trimmed storage");
    } catch (StorageManagerException e) {
      logger.warn("Failed to trim storage. {}", e.getMessage());
      routingContext.response().setStatusCode(500).end("Failed trim storage. Error: " + e.getMessage());
    }
  }

  private void getFileHistory(RoutingContext routingContext) {
    String encodedFile = routingContext.request().getParam("file");
    if (encodedFile.isEmpty()) {
      routingContext.response().setStatusCode(400).end("Must include file parameter.");
    } else {
      try {
        String file = URLDecoder.decode(encodedFile, "UTF-8");
        List<JournalEntry> fileHistory = backupManager.getFileHistory(Paths.get(file));
        List<FileHistoryEntry> fileHistoryEntries = fileHistory.stream()
                .map(x -> new FileHistoryEntry(x.getShardInfo().getModificationDateTime().getMillis(), x.getShardInfo().getMD5HashList(), x.getShardInfo().getTotalSize(), x.getFileOperation().equals(FileOperation.REMOVE)))
                .collect(Collectors.toList());
        MembraneFileHistory membraneFileHistory = new MembraneFileHistory(fileHistoryEntries, file);
        sendObject(routingContext, membraneFileHistory);
      } catch (UnsupportedEncodingException e) {
        logger.warn("Could not decode due to unsupported encoding UTF-8");
        routingContext.response().setStatusCode(500).end("Could not decode argument. Error: " + e.getMessage());
      }
    }
  }

  private void reconstructFile(RoutingContext routingContext) {
    MultiMap params = routingContext.request().params();
    if (!params.contains("file") || !params.contains("target")) {
      String encodedFile = params.get("file");
      String encodedTarget = params.get("target");
      Optional<String> dateTimeMillisOptional = params.contains("dateTime") ? Optional.of(params.get("dateTime")) : Optional.empty();
      try {
        Path file = Paths.get(URLDecoder.decode(encodedFile, "UTF-8"));
        Path target = Paths.get(URLDecoder.decode(encodedTarget, "UTF-8"));
        if (dateTimeMillisOptional.isPresent()) {
          DateTime dateTime = new DateTime(Long.parseLong(dateTimeMillisOptional.get()));
          backupManager.recoverFile(file, target, dateTime);
        } else {
          backupManager.recoverFile(file, target);
        }
      } catch (UnsupportedEncodingException e) {
        logger.warn("Could not decode due to unsupported encoding UTF-8");
        routingContext.response().setStatusCode(500).end("Could not decode argument. Error: " + e.getMessage());
      } catch (NumberFormatException e) {
        logger.warn("Invalid Number Format given");
        routingContext.response().setStatusCode(400).end("Invalid dateTime received. Error: " + e.getMessage());
      } catch (StorageManagerException e) {
        logger.warn("Could not reconstruct file: {}. {}", encodedFile, e.getMessage());
        routingContext.response().setStatusCode(400).end("File given. Error: " + e.getMessage());
      }
    } else {
      logger.warn("Invalid file reconstruct request received. No file and target field.");
      routingContext.response().setStatusCode(400).end("Invalid request. Must contain file and target fields.");
    }
  }
}
