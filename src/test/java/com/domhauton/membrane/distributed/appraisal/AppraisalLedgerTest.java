package com.domhauton.membrane.distributed.appraisal;

import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by dominic on 12/04/17.
 */
class AppraisalLedgerTest {

  private static final String PEER_ID_1 = "Peer_1";
  private static final String PEER_ID_2 = "Peer_2";
  private static final String BLOCK_ID_1 = "Block_1";
  private static final String BLOCK_ID_2 = "Block_2";
  private static final String BLOCK_ID_3 = "Block_3";

  private Path basePath;

  @BeforeEach
  void setUp() throws Exception {
    basePath = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));

  }

  @Test
  void simpleTest() throws Exception {
    AppraisalLedger appraisalLedger = new AppraisalLedger(basePath);
    appraisalLedger.run();

    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now(), 1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 1);

    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now(), 1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 1);

    double peerRating1 = appraisalLedger.getPeerRating(PEER_ID_1);
    double peerRating2 = appraisalLedger.getPeerRating(PEER_ID_2);
    Assertions.assertEquals(peerRating1, peerRating2);
    appraisalLedger.close();
  }

  @Test
  void deduplicationTest() throws Exception {
    AppraisalLedger appraisalLedger = new AppraisalLedger(basePath);
    appraisalLedger.run();

    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now(), 1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now(), 1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 1);

    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now(), 1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 1);
    double peerRating1 = appraisalLedger.getPeerRating(PEER_ID_1);
    double peerRating2 = appraisalLedger.getPeerRating(PEER_ID_2);
    Assertions.assertEquals(peerRating1, peerRating2);
    appraisalLedger.close();
  }

  @Test
  void blockDeduplicationTest() throws Exception {
    AppraisalLedger appraisalLedger = new AppraisalLedger(basePath);
    appraisalLedger.run();

    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusMinutes(2), 1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(2), 2);

    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusMinutes(2), 1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(2), 2);

    double peerRating1 = appraisalLedger.getPeerRating(PEER_ID_1);
    double peerRating2 = appraisalLedger.getPeerRating(PEER_ID_2);
    Assertions.assertEquals(peerRating1, peerRating2);
    appraisalLedger.close();
  }

  @Test
  void incompleteReportPenaltyTest() throws Exception {
    AppraisalLedger appraisalLedger = new AppraisalLedger(basePath);
    appraisalLedger.run();

    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusMinutes(2), 0);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2, BLOCK_ID_2);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(2), 2);

    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusMinutes(2), 0);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(2), 2);

    double peerRating1 = appraisalLedger.getPeerRating(PEER_ID_1, DateTime.now().plusHours(2));
    double peerRating2 = appraisalLedger.getPeerRating(PEER_ID_2, DateTime.now().plusHours(2));
    Assertions.assertTrue(peerRating1 > peerRating2);
    appraisalLedger.close();
  }

  @Test
  void lostBlockPenaltyTest() throws Exception {
    AppraisalLedger appraisalLedger = new AppraisalLedger(basePath);
    appraisalLedger.run();

    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusMinutes(2), 1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 2, BLOCK_ID_2);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(2), 2);

    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusMinutes(2), 1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2, BLOCK_ID_1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2, BLOCK_ID_2);

    appraisalLedger.registerLostBlock(PEER_ID_2, DateTime.now().plusHours(1), 2);

    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 2, BLOCK_ID_3);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(2), 2);

    double peerRating1 = appraisalLedger.getPeerRating(PEER_ID_1, DateTime.now().plusHours(2));
    double peerRating2 = appraisalLedger.getPeerRating(PEER_ID_2, DateTime.now().plusHours(2));
    System.out.println(peerRating1 + " " + peerRating2);
    Assertions.assertTrue(peerRating1 > peerRating2);
    appraisalLedger.close();
  }

  @Test
  void simplePersistTest() throws Exception {
    basePath = Paths.get(basePath.toString() + File.separator + "inner_dir");
    AppraisalLedger appraisalLedger = new AppraisalLedger(basePath);
    appraisalLedger.run();

    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now(), 1);
    appraisalLedger.registerPeerContact(PEER_ID_1, DateTime.now().plusHours(1), 1);

    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now(), 1);
    appraisalLedger.registerPeerContact(PEER_ID_2, DateTime.now().plusHours(1), 1);

    appraisalLedger.writeAppraisals();

    appraisalLedger.close();

    AppraisalLedger appraisalLedger2 = new AppraisalLedger(basePath);

    double peerRating1 = appraisalLedger2.getPeerRating(PEER_ID_1);
    double peerRating2 = appraisalLedger2.getPeerRating(PEER_ID_2);
    Assertions.assertEquals(peerRating1, peerRating2);
    appraisalLedger2.close();
  }

  @AfterEach
  void tearDown() throws Exception {
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath);
  }
}