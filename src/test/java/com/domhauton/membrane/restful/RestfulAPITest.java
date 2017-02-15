package com.domhauton.membrane.restful;

import com.domhauton.membrane.BackupManager;
import com.domhauton.membrane.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by dominic on 03/02/17.
 */
class RestfulAPITest {

  private RestfulAPI viewVerticle;
  private BackupManager backupManager;

  @BeforeEach
  void setUp() throws Exception {
    backupManager = Mockito.mock(BackupManager.class);
    Mockito.when(backupManager.getConfigPath()).thenReturn(Paths.get("/tmp/membrane/membrane.yaml"));
    Mockito.when(backupManager.getConfig()).thenReturn(ConfigManager.loadDefaultConfig());
    Mockito.when(backupManager.getWatchedFiles())
            .thenReturn(new HashSet<>(
                    Arrays.asList("/tmp/membrane/watchfolder1/file1.txt",
                            "/tmp/membrane/watchfolder1/file2.txt",
                            "/tmp/membrane/watchfolder2/file3.txt")));
    Mockito.when(backupManager.getCurrentFiles())
            .thenReturn(new HashSet<>(
                    Arrays.asList(Paths.get("/tmp/membrane/watchfolder1/file1.txt"),
                            Paths.get("/tmp/membrane/watchfolder1/file2.txt"),
                            Paths.get("/tmp/membrane/watchfolder2/file3.txt"))));
    Mockito.when(backupManager.getReferencedFiles())
            .thenReturn(new HashSet<>(
                    Arrays.asList(Paths.get("/tmp/membrane/watchfolder1/file1.txt"),
                            Paths.get("/tmp/membrane/watchfolder1/file2.txt"),
                            Paths.get("/tmp/membrane/watchfolder1/file2.txt.bkp"),
                            Paths.get("/tmp/membrane/watchfolder2/file3.txt"))));
    Mockito.when(backupManager.getWatchedFolders())
            .thenReturn(new HashSet<>(Arrays.asList("/tmp/membrane/watchfolder1", "/tmp/membrane/watchfolder2")));
    Mockito.when(backupManager.getStorageSize())
            .thenReturn(1024L);
    viewVerticle = new RestfulAPI(13300, backupManager);
  }

  @Test
  void waitTest() throws Exception {
    viewVerticle.start();
    //Thread.sleep(20 * 1000);
  }

  @AfterEach
  void tearDown() {
    viewVerticle.close();
  }
}