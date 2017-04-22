package com.domhauton.membrane.api;

import com.domhauton.membrane.BackupManager;
import com.domhauton.membrane.api.requests.FileID;
import com.domhauton.membrane.api.requests.WatchFolderChange;
import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.config.items.data.WatchFolder;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.impl.RoutingContextImpl;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.Mockito.*;

/**
 * Created by dominic on 03/02/17.
 */
class RestfulApiManagerTest {

  private RestfulApiManager restfulApiManager;
  private BackupManager backupManager;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    backupManager = mock(BackupManager.class);
    when(backupManager.getConfigPath()).thenReturn(Paths.get("/tmp/membrane/membrane.yaml"));
    Config config =ConfigManager.loadDefaultConfig();
    config.getFileWatcher().getFolders().add(new WatchFolder("/tmp/foobar", true));
    when(backupManager.getConfig()).thenReturn(config);
    when(backupManager.getWatchedFiles())
            .thenReturn(new HashSet<>(
                    Arrays.asList("/tmp/membrane/watchfolder1/file1.txt",
                            "/tmp/membrane/watchfolder1/file2.txt",
                            "/tmp/membrane/watchfolder2/file3.txt")));
    when(backupManager.getCurrentFiles())
            .thenReturn(new HashSet<>(
                    Arrays.asList(Paths.get("/tmp/membrane/watchfolder1/file1.txt"),
                            Paths.get("/tmp/membrane/watchfolder1/file2.txt"),
                            Paths.get("/tmp/membrane/watchfolder2/file3.txt"))));
    when(backupManager.getReferencedFiles())
            .thenReturn(new HashSet<>(
                    Arrays.asList(Paths.get("/tmp/membrane/watchfolder1/file1.txt"),
                            Paths.get("/tmp/membrane/watchfolder1/file2.txt"),
                            Paths.get("/tmp/membrane/watchfolder1/file2.txt.bkp"),
                            Paths.get("/tmp/membrane/watchfolder2/file3.txt"))));
    when(backupManager.getWatchedFolders())
            .thenReturn(new HashSet<>(Arrays.asList("/tmp/membrane/watchfolder1", "/tmp/membrane/watchfolder2")));
    when(backupManager.getStorageSize())
            .thenReturn(1024L);
    restfulApiManager = new RestfulApiManager(13300, backupManager);
  }

  @Test
  void launchTest() throws Exception {
    restfulApiManager.start();
  }

  @Test
  void portTakenTest() throws Exception {
    restfulApiManager.start();
    RestfulApiManager restfulApiManager2 = new RestfulApiManager(13300, backupManager);
    Assertions.assertThrows(RestfulApiException.class, restfulApiManager2::start);
    restfulApiManager2.close();
  }

  @Test
  void ipFilteringRejectTest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();
    when(routingContext.request().remoteAddress().host()).thenReturn("127.0.0.2");
    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    restfulApiManager.hostFilter(routingContext);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(403);
  }

  @Test
  void ipFilteringAllowTest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();
    when(routingContext.request().remoteAddress().host()).thenReturn("127.0.0.1");
    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    restfulApiManager.hostFilter(routingContext);
    verify(routingContext, atLeastOnce()).next();
  }

  @Test
  void testSuccessfulRootHandle() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    when(backupManager.isMonitorMode()).thenReturn(true);
    when(backupManager.getStartTime()).thenReturn(DateTime.now());

    restfulApiManager.rootHandler(routingContext);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(200);
  }

  @Test
  void testSuccessfulWatchFoldersRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    restfulApiManager.getConfiguredWatchFolders(routingContext);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(200);
  }

  @Test
  void testSuccessfulWatchFolderRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    restfulApiManager.getFileWatcherStatus(routingContext);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(200);
  }

  @Test
  void testSuccessfulStorageStatusRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    restfulApiManager.getStorageStatus(routingContext);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(200);
  }

  @Test
  void testSuccessfulWatchFolderAdd() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    WatchFolder watchFolder = new WatchFolder("/tmp/folder", true);
    WatchFolderChange watchFolderChange = new WatchFolderChange(WatchFolderChange.Type.ADD, watchFolder);

    String requestBody = objectMapper.writeValueAsString(watchFolderChange);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    restfulApiManager.modifyWatchFolder(routingContext);

    verify(backupManager, atLeastOnce()).addWatchFolder(watchFolder);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(200);
  }

  @Test
  void testSuccessfulWatchFolderRemove() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    WatchFolder watchFolder = new WatchFolder("/tmp/folder", true);
    WatchFolderChange watchFolderChange = new WatchFolderChange(WatchFolderChange.Type.REMOVE, watchFolder);

    String requestBody = objectMapper.writeValueAsString(watchFolderChange);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    restfulApiManager.modifyWatchFolder(routingContext);

    verify(backupManager, atLeastOnce()).removeWatchFolder(watchFolder);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(200);
  }

  @Test
  void testExistingWatchFolderAdd() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    WatchFolder watchFolder = new WatchFolder("/tmp/folder", true);
    WatchFolderChange watchFolderChange = new WatchFolderChange(WatchFolderChange.Type.ADD, watchFolder);

    String requestBody = objectMapper.writeValueAsString(watchFolderChange);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    doThrow(new IllegalArgumentException("Mockito Forced Fail")).when(backupManager).addWatchFolder(watchFolder);

    restfulApiManager.modifyWatchFolder(routingContext);

    verify(backupManager, atLeastOnce()).addWatchFolder(watchFolder);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(400);
  }

  @Test
  void testWatchFolderAddPersistFail() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    WatchFolder watchFolder = new WatchFolder("/tmp/folder", true);
    WatchFolderChange watchFolderChange = new WatchFolderChange(WatchFolderChange.Type.ADD, watchFolder);

    String requestBody = objectMapper.writeValueAsString(watchFolderChange);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(anyInt())).thenReturn(httpServerResponse);

    doThrow(new ConfigException("Mockito Forced Config Save Fail")).when(backupManager).addWatchFolder(watchFolder);

    restfulApiManager.modifyWatchFolder(routingContext);

    verify(backupManager, atLeastOnce()).addWatchFolder(watchFolder);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(304);
  }

  @Test
  void testWatchFolderAddInvalidJSON() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    WatchFolder watchFolder = new WatchFolder("/tmp/folder", true);
    WatchFolderChange watchFolderChange = new WatchFolderChange(WatchFolderChange.Type.ADD, watchFolder);

    String requestBody = objectMapper.writeValueAsString(watchFolderChange);

    when(routingContext.getBodyAsString()).thenReturn(requestBody.substring(1));

    restfulApiManager.modifyWatchFolder(routingContext);

    verify(backupManager, atMost(0)).addWatchFolder(watchFolder);
    verify(httpServerResponse, atLeastOnce()).setStatusCode(400);
  }

  @Test
  void testSuccessfulCleanupRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    restfulApiManager.putRequestCleanup(routingContext);

    verify(backupManager, atLeastOnce()).trimStorageAttempt();
    verify(httpServerResponse, atLeastOnce()).setStatusCode(200);
  }

  @Test
  void testFailedCleanupRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    doThrow(new StorageManagerException("MOCKITO FORCED THROW")).when(backupManager).trimStorageAttempt();

    restfulApiManager.putRequestCleanup(routingContext);

    verify(backupManager, atLeastOnce()).trimStorageAttempt();
    verify(httpServerResponse, atLeastOnce()).setStatusCode(500);
  }

  @Test
  void testSuccessfulFileHistoryRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(200)).thenReturn(httpServerResponse);

    FileID fileID = new FileID("/tmp/file1.txt");

    String requestBody = objectMapper.writeValueAsString(fileID);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    when(backupManager.getFileHistory(Paths.get(fileID.getFilepath())))
            .thenReturn(Arrays.asList(
                    new JournalEntry(DateTime.now(),
                            new FileVersion(
                                    Arrays.asList(
                                            new MD5HashLengthPair("statarstarst", 20),
                                            new MD5HashLengthPair("asrtdatsrdqa", 20)),
                                    DateTime.now()
                            ), FileOperation.ADD,
                            Paths.get(fileID.getFilepath())),
                    new JournalEntry(DateTime.now(),
                            new FileVersion(
                                    Arrays.asList(
                                            new MD5HashLengthPair("statarstarst", 20),
                                            new MD5HashLengthPair("arstasrdttsa", 22)),
                                    DateTime.now()
                            ), FileOperation.ADD,
                            Paths.get(fileID.getFilepath()))));

    restfulApiManager.getFileHistory(routingContext);

    verify(backupManager, atLeastOnce()).getFileHistory(Paths.get(fileID.getFilepath()));
    verify(httpServerResponse, atLeast(1)).setStatusCode(200);
  }

  @Test
  void testFailedFileHistoryRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(400)).thenReturn(httpServerResponse);

    FileID fileID = new FileID("/tmp/file1.txt");

    String requestBody = objectMapper.writeValueAsString(fileID);

    when(routingContext.getBodyAsString()).thenReturn(requestBody.substring(6));

    restfulApiManager.getFileHistory(routingContext);

    verify(backupManager, atMost(0)).getFileHistory(Mockito.any());
    verify(httpServerResponse, atLeast(1)).setStatusCode(400);
  }

  @Test
  void testSuccessfulFileReconstructRequestTime() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(200)).thenReturn(httpServerResponse);

    FileID fileID = new FileID("/tmp/file1.txt", "/tmp/file2.txt", System.currentTimeMillis());

    String requestBody = objectMapper.writeValueAsString(fileID);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    restfulApiManager.reconstructFile(routingContext);

    verify(backupManager, atLeastOnce())
            .recoverFile(
                    Paths.get(fileID.getFilepath()),
                    Paths.get(fileID.getTargetFilePath()),
                    new DateTime(fileID.getDateTimeMillis()));
    verify(httpServerResponse, times(1)).setStatusCode(200);
  }

  @Test
  void testSuccessfulFileReconstructRequestNoTime() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(200)).thenReturn(httpServerResponse);

    FileID fileID = new FileID("/tmp/file1.txt", "/tmp/file2.txt", -1);

    String requestBody = objectMapper.writeValueAsString(fileID);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    restfulApiManager.reconstructFile(routingContext);

    verify(backupManager, atLeastOnce())
            .recoverFile(Paths.get(fileID.getFilepath()), Paths.get(fileID.getTargetFilePath()));
    verify(httpServerResponse, times(1)).setStatusCode(200);
  }

  @Test
  void testFailedFileReconstructRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(500)).thenReturn(httpServerResponse);

    FileID fileID = new FileID("/tmp/file1.txt", "/tmp/file2.txt", -1);

    String requestBody = objectMapper.writeValueAsString(fileID);

    when(routingContext.getBodyAsString()).thenReturn(requestBody);

    doThrow(new StorageManagerException("Mockito Forced Recovery Fail"))
            .when(backupManager)
            .recoverFile(Paths.get(fileID.getFilepath()), Paths.get(fileID.getTargetFilePath()));

    restfulApiManager.reconstructFile(routingContext);

    verify(backupManager, atLeastOnce())
            .recoverFile(Paths.get(fileID.getFilepath()), Paths.get(fileID.getTargetFilePath()));
    verify(httpServerResponse, times(1)).setStatusCode(500);
  }

  @Test
  void testFailedFileReconstructInvalidRequest() throws Exception {
    RoutingContextImpl routingContext = mock(RoutingContextImpl.class, RETURNS_DEEP_STUBS);
    HttpServerResponse httpServerResponse = routingContext.response();

    when(routingContext.response().putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(routingContext.response().setStatusCode(500)).thenReturn(httpServerResponse);

    FileID fileID = new FileID("/tmp/file1.txt", "/tmp/file2.txt", -1);

    String requestBody = objectMapper.writeValueAsString(fileID);

    when(routingContext.getBodyAsString()).thenReturn(requestBody.substring(13));

    restfulApiManager.reconstructFile(routingContext);

    verify(backupManager, times(0)).recoverFile(Mockito.any(), Mockito.any());
    verify(httpServerResponse, times(1)).setStatusCode(400);
  }

  @AfterEach
  void tearDown() {
    restfulApiManager.close();
  }
}