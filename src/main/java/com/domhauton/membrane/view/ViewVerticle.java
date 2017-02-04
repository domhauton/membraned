package com.domhauton.membrane.view;

import com.domhauton.membrane.BackupManager;
import com.domhauton.membrane.view.responses.FileManagerStatus;
import com.domhauton.membrane.view.responses.MembraneStatus;
import com.domhauton.membrane.view.responses.MembraneInfo;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * Created by dominic on 03/02/17.
 */
public class ViewVerticle {
    private final Logger logger;
    private final int port;
    private final Vertx vertx;
    private final HttpServer httpServer;
    private final Router router;
    private final ObjectMapper objectMapper;

    private final BackupManager backupManager;

    public ViewVerticle(int port, BackupManager backupManager)  {
        logger = LogManager.getLogger();
        this.port = port;
        this.backupManager = backupManager;
        vertx = Vertx.vertx();
        httpServer = vertx.createHttpServer();
        router = Router.router(vertx);
        objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY);
    }

    public void start() {
        router.route("/").handler(this::rootHandler);
        router.route("/config").blockingHandler(this::getMembraneConfig);
        router.route("/watcher").blockingHandler(this::getFileWatcherStatus);

        httpServer.requestHandler(router::accept).listen(port);
    }

    public void close() {
        router.clear();
        httpServer.close();
    }

    private void rootHandler(RoutingContext routingContext) {
        MembraneInfo runtimeInfo = new MembraneInfo(port, "0.2.0", MembraneStatus.GOOD, "Welcome to Membrane!");
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
        try {
            String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backupManager.getConfig());
            logger.info("Sending response to {}", routingContext.request().remoteAddress().host());
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(response);
        } catch (JsonProcessingException e) {
            logger.error("Could not generate JSON config response. {}", e.getMessage());
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(500)
                    .end("Unable to generate response. Please check the logs.\n" + e.getMessage());
        }
    }

    private void getFileWatcherStatus(RoutingContext routingContext) {
        Set<String> watchedFolders = backupManager.getWatchedFolders();
        Set<String> watchedFiles = backupManager.getWatchedFiles();
        FileManagerStatus fileManagerStatus = new FileManagerStatus(watchedFolders, watchedFiles);
        try {
            String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fileManagerStatus);
            logger.info("Sending file manager status to {}", routingContext.request().remoteAddress().host());
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(response);
        } catch (JsonProcessingException e) {
            logger.error("Could not generate file manager status response. {}", e.getMessage());
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(500)
                    .end("Unable to generate response. Please check the logs.\n" + e.getMessage());
        }
    }
}
