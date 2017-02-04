package com.domhauton.membrane.view;

import com.domhauton.membrane.BackupManager;
import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by dominic on 03/02/17.
 */
class ViewVerticleTest {

    private ViewVerticle viewVerticle;
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
        Mockito.when(backupManager.getWatchedFolders())
                .thenReturn(new HashSet<>(Arrays.asList("/tmp/membrane/watchfolder1", "/tmp/membrane/watchfolder2")));
        viewVerticle = new ViewVerticle(13300, backupManager);
    }

    @Test
    void waitTest() throws Exception {
        viewVerticle.start();
        Thread.sleep(20 * 1000);
    }

    @AfterEach
    void tearDown() {
        viewVerticle.close();
    }
}