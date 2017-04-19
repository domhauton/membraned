package com.domhauton.membrane.distributed;

import com.domhauton.membrane.network.NetworkManager;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageImpl;
import com.domhauton.membrane.storage.BackupLedger;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by dominic on 19/04/17.
 */
class DistributorTest {
  private static long GIGABYTE = 1024 * 1024 * 1024;
  private static int CONTRACT_LIMIT = 20;

  private Distributor distributor;
  private Path basePath;
  private BackupLedger backupLedgerMock;
  private ShardStorage localShardStorage;
  private ShardStorage peerShardStorage;
  private NetworkManager networkManagerMock;


  @BeforeEach
  void setUp() throws Exception {
    basePath = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));
    backupLedgerMock = Mockito.mock(BackupLedger.class);
    Path localShardStoragePath = Paths.get(backupLedgerMock.toString() + File.separator + "/localShards");
    Path peerShardStoragePath = Paths.get(backupLedgerMock.toString() + File.separator + "/peerShards");
    localShardStorage = new ShardStorageImpl(localShardStoragePath, GIGABYTE);
    peerShardStorage = new ShardStorageImpl(peerShardStoragePath, GIGABYTE, Hashing.sha512());
    networkManagerMock = Mockito.mock(NetworkManager.class);

    distributor = new Distributor(basePath, backupLedgerMock, localShardStorage, peerShardStorage, networkManagerMock, CONTRACT_LIMIT);
  }

  @Test
  void startStopTest() throws Exception {
    distributor.run();
    Thread.sleep(1000);
    distributor.close();
  }

  @AfterEach
  void tearDown() throws Exception {
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath);
  }
}