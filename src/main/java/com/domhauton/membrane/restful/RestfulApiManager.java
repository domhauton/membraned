package com.domhauton.membrane.restful;

import com.domhauton.membrane.BackupManager;
import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.config.items.*;
import com.domhauton.membrane.restful.requests.FileID;
import com.domhauton.membrane.restful.requests.WatchFolderChange;
import com.domhauton.membrane.restful.responses.*;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Created by dominic on 03/02/17.
 */
public class RestfulApiManager implements Closeable {
  private static final long BODY_LIMIT = 1024 * 1024 * 64;
  private static final String ALLOWED_HOST = "127.0.0.1";

  private final Logger logger;
  private final int port;
  private final Vertx vertx;
  private final HttpServer httpServer;
  private final Router router;
  private final ObjectMapper objectMapper;

  private final BackupManager backupManager;

  public RestfulApiManager(int port, BackupManager backupManager) {
    logger = LogManager.getLogger();
    this.port = port;
    this.backupManager = backupManager;
    vertx = Vertx.vertx();
    httpServer = vertx.createHttpServer();
    router = Router.router(vertx);
    router.route().handler(BodyHandler.create().setBodyLimit(BODY_LIMIT));
    objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY);
  }

  public void start() throws RestfulApiException {
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

    CompletableFuture<Boolean> startUpListener = new CompletableFuture<>();

    httpServer.requestHandler(router::accept).listen(port, handler -> {
      startUpListener.complete(handler.succeeded());
      if (!handler.succeeded()) {
        logger.error("REST Listener failed to start. {}", handler.cause().getMessage());
      }
    });

    try {
      if (startUpListener.get(5, TimeUnit.SECONDS)) {
        logger.info("Listening at localhost:{}", port);
      } else {
        throw new RestfulApiException("Failed to start listener. Check if port is busy");
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.fatal("Could not start Restful Api listener after 5 seconds.");
      throw new RestfulApiException("Failed to start listener. Check if port is busy");
    }
  }

  public void close() {
    router.clear();
    httpServer.close();
  }

  void hostFilter(RoutingContext routingContext) {
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

  void rootHandler(RoutingContext routingContext) {
    MembraneStatus membraneStatus = backupManager.isMonitorMode() ?
            MembraneStatus.MONITOR_MODE : MembraneStatus.NORMAL;
    String version = backupManager.getVersion();
    DateTime startTime = backupManager.getStartTime();

    MembraneInfo runtimeInfo = new MembraneInfo(port, startTime, version, membraneStatus, "Welcome to Membrane!");
    sendObject(routingContext, runtimeInfo);
  }

  void getMembraneConfig(RoutingContext routingContext) {
    Config config = backupManager.getConfig();
    MembraneRestConfig membraneRestConfig = new MembraneRestConfig(config);
    logger.info("Sending config status to {}", routingContext.request().remoteAddress().host());
    sendObject(routingContext, membraneRestConfig);
  }

  void getFileWatcherStatus(RoutingContext routingContext) {
    Set<String> watchedFolders = backupManager.getWatchedFolders();
    Set<String> watchedFiles = backupManager.getWatchedFiles();
    FileManagerStatus fileManagerStatus = new FileManagerStatus(watchedFolders, watchedFiles);
    logger.info("Sending file watcher status to {}", routingContext.request().remoteAddress().host());
    sendObject(routingContext, fileManagerStatus);
  }

  void getStorageStatus(RoutingContext routingContext) {
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

  void putNewConfig(RoutingContext routingContext) {
    Path configPath = backupManager.getConfigPath();
    try {
      MembraneRestConfig membraneRestConfig = Json.decodeValue(routingContext.getBodyAsString(), MembraneRestConfig.class);
      WatcherConfig watcherConfig = new WatcherConfig(
              membraneRestConfig.getWatcher().getChunkSize(),
              membraneRestConfig.getWatcher().getWatchFolders()
                      .stream()
                      .map(x -> new WatchFolder(x.getDirectory(), x.isRecursive()))
                      .collect(Collectors.toList()),
              membraneRestConfig.getWatcher().getFileRescan(),
              membraneRestConfig.getWatcher().getFolderRescan());

      LocalStorageConfig localStorageConfig = new LocalStorageConfig(
              membraneRestConfig.getLocalStorage().getDirectory(),
              membraneRestConfig.getLocalStorage().getTrimFrequency(),
              membraneRestConfig.getLocalStorage().getSoftStorageCap(),
              membraneRestConfig.getLocalStorage().getHardStorageCap());

      DistributedStorageConfig distributedStorageConfig = new DistributedStorageConfig(
              membraneRestConfig.getDistributedStorage().getDirectory(),
              membraneRestConfig.getDistributedStorage().getTrimFrequency(),
              membraneRestConfig.getDistributedStorage().getSoftStorageCap(),
              membraneRestConfig.getDistributedStorage().getHardStorageCap(),
              membraneRestConfig.getDistributedStorage().getTransportPort(),
              membraneRestConfig.getDistributedStorage().getExternalTransportPort(),
              membraneRestConfig.getDistributedStorage().isNatForwardingEnabled());

      RestConfig restConfig = new RestConfig(membraneRestConfig.getRestAPI().getPort());

      Config config = new Config(
              distributedStorageConfig,
              localStorageConfig,
              watcherConfig,
              restConfig);

      ConfigManager.saveConfig(configPath, config);
      logger.info("Successfully wrote new config.");
      routingContext.response().setStatusCode(200).end("Successfully wrote config to: " + configPath.toString());

    } catch (DecodeException e) {
      logger.warn("Failed to decode body. {}", e.getMessage());
      routingContext.response().setStatusCode(400).end("Failed to decode request. Error: " + e.getMessage());
    } catch (ConfigException e) {
      logger.warn("Failed to write new config. {}", e.getMessage());
      routingContext.response().setStatusCode(500).end("Failed to write config to: " + configPath.toString() + "Error: " + e.getMessage());
    }
  }

  void modifyWatchFolder(RoutingContext routingContext) {
    Path configPath = backupManager.getConfigPath();
    try {
      final WatchFolderChange watchFolderChange = Json.decodeValue(routingContext.getBodyAsString(), WatchFolderChange.class);
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
    } catch (DecodeException e) {
      logger.warn("Failed to decode body. {}", e.getMessage());
      routingContext.response().setStatusCode(400).end("Failed to decode request. Error: " + e.getMessage());
    }
  }

  void putRequestCleanup(RoutingContext routingContext) {
    try {
      backupManager.trimStorageAttempt();
      routingContext.response().setStatusCode(200).end("Successfully trimmed storage");
    } catch (StorageManagerException e) {
      logger.warn("Failed to trim storage. {}", e.getMessage());
      routingContext.response().setStatusCode(500).end("Failed trim storage. Error: " + e.getMessage());
    }
  }

  void getFileHistory(RoutingContext routingContext) {
    try {
      final FileID fileID = Json.decodeValue(routingContext.getBodyAsString(), FileID.class);
      List<JournalEntry> fileHistory = backupManager.getFileHistory(Paths.get(fileID.getFilepath()));
      List<FileHistoryEntry> fileHistoryEntries = fileHistory.stream()
              .map(x -> new FileHistoryEntry(x.getShardInfo().getModificationDateTime().getMillis(), x.getShardInfo().getMD5HashList(), x.getShardInfo().getTotalSize(), x.getFileOperation().equals(FileOperation.REMOVE)))
              .collect(Collectors.toList());
      MembraneFileHistory membraneFileHistory = new MembraneFileHistory(fileHistoryEntries, fileID.getFilepath());
      sendObject(routingContext, membraneFileHistory);
    } catch (DecodeException e) {
      logger.warn("Invalid file history request");
      routingContext.response().setStatusCode(400).end("Could not decode argument. Error: " + e.getMessage());
    }
  }

  void reconstructFile(RoutingContext routingContext) {
    try {
      final FileID fileID = Json.decodeValue(routingContext.getBodyAsString(), FileID.class);
      Path file = Paths.get(fileID.getFilepath());
      Path target = Paths.get(fileID.getTargetFilePath());
      if (fileID.getDateTimeMillis() >= 0) {
        DateTime dateTime = new DateTime(fileID.getDateTimeMillis());
        backupManager.recoverFile(file, target, dateTime);
      } else {
        backupManager.recoverFile(file, target);
      }
      routingContext.response().setStatusCode(200).end("Successfully reconstructed file.");
    } catch (StorageManagerException e) {
      logger.warn("Could not reconstruct file. {}", e.getMessage());
      routingContext.response().setStatusCode(500).end("File given. Error: " + e.getMessage());
    } catch (DecodeException e) {
      logger.warn("Invalid file history request");
      routingContext.response().setStatusCode(400).end("Could not decode argument. Error: " + e.getMessage());
    }
  }
}
