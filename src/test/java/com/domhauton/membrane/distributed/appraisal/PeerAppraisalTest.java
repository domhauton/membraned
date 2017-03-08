package com.domhauton.membrane.distributed.appraisal;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by Dominic Hauton on 05/03/17.
 */
class PeerAppraisalTest {

  @Test
  void peerId() throws Exception {
    DateTime baseDateTime = DateTime.now();
    PeerAppraisal peerAppraisal = new PeerAppraisal("dummyPeer1", baseDateTime);
    Assertions.assertEquals("dummyPeer1", peerAppraisal.getPeerId());
  }

  @Test
  void ensureDefaultToZeroTest() throws Exception {
    DateTime baseDateTime = DateTime.now();
    PeerAppraisal peerAppraisal = new PeerAppraisal("dummyPeer1", baseDateTime);
    double[] emptyDistribution = peerAppraisal.getShardReturnDistribution(baseDateTime);

    for (double val : emptyDistribution) {
      Assertions.assertEquals(0.0d, val);
    }

    peerAppraisal.flushHourlyReportIntake();
    Assertions.assertEquals(1.0d, peerAppraisal.getContractSuccessChance());
  }

  @Test
  void assertCorrectReturnTest() throws Exception {
    DateTime baseDateTime = DateTime.now();
    PeerAppraisal peerAppraisal = new PeerAppraisal("dummyPeer1", baseDateTime);
    peerAppraisal.registerReport(baseDateTime.plusHours(1), 0, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard1");
    double[] emptyDistribution = peerAppraisal.getShardReturnDistribution(baseDateTime.plusWeeks(1));

    int zeroCnt = 0;
    int halfCnt = 0;
    int oneCnt = 0;

    for (double val : emptyDistribution) {
      if (val == 0.0d) {
        zeroCnt++;
      } else if (val == 0.5d) {
        halfCnt++;
      } else if (val == 1.0d) {
        oneCnt++;
      } else {
        Assertions.fail("Value should not exist: " + val);
      }
    }

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - 2, zeroCnt);
    Assertions.assertEquals(1, oneCnt);
    Assertions.assertEquals(1, halfCnt);

    peerAppraisal.flushHourlyReportIntake();
    Assertions.assertEquals(0.5d, peerAppraisal.getContractSuccessChance());
  }

  @Test
  void assertCorrectReturnFullTest() throws Exception {
    DateTime baseDateTime = DateTime.now();
    PeerAppraisal peerAppraisal = new PeerAppraisal("dummyPeer1", baseDateTime);
    peerAppraisal.registerReport(baseDateTime.plusHours(1), 0, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard2");
    double[] emptyDistribution = peerAppraisal.getShardReturnDistribution(baseDateTime.plusWeeks(1));

    int zeroCnt = 0;
    int oneCnt = 0;

    for (double val : emptyDistribution) {
      if (val == 0.0d) {
        zeroCnt++;
      } else if (val == 1.0d) {
        oneCnt++;
      } else {
        Assertions.fail("Value should not exist: " + val);
      }
    }

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - 2, zeroCnt);
    Assertions.assertEquals(2, oneCnt);

    peerAppraisal.flushHourlyReportIntake();
    Assertions.assertEquals(1.0d, peerAppraisal.getContractSuccessChance());
  }

  @Test
  void assertCorrectFourWeeksTest() throws Exception {
    DateTime baseDateTime = DateTime.now();
    PeerAppraisal peerAppraisal = new PeerAppraisal("dummyPeer1", baseDateTime);
    peerAppraisal.registerReport(baseDateTime.plusHours(1), 0, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard2");
    double[] emptyDistribution = peerAppraisal.getShardReturnDistribution(baseDateTime.plusWeeks(4));

    int zeroCnt = 0;
    int quarterCnt = 0;

    for (double val : emptyDistribution) {
      if (val == 0.0d) {
        zeroCnt++;
      } else if (val == 0.25d) {
        quarterCnt++;
      } else {
        Assertions.fail("Value should not exist: " + val);
      }
    }

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - 2, zeroCnt);
    Assertions.assertEquals(2, quarterCnt);

    peerAppraisal.flushHourlyReportIntake();
    Assertions.assertEquals(1.0d, peerAppraisal.getContractSuccessChance());
  }

  @Test
  void assertCorrectFourWeeksFalseTest() throws Exception {
    DateTime baseDateTime = DateTime.now();
    PeerAppraisal peerAppraisal = new PeerAppraisal("dummyPeer1", baseDateTime);
    peerAppraisal.registerReport(baseDateTime.plusHours(1), 0, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2);
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard1");
    double[] emptyDistribution = peerAppraisal.getShardReturnDistribution(baseDateTime.plusWeeks(4));

    int zeroCnt = 0;
    int eighthCnt = 0;
    int quartCnt = 0;

    for (double val : emptyDistribution) {
      if (val == 0.0d) {
        zeroCnt++;
      } else if (val == 0.125d) {
        eighthCnt++;
      } else if (val == 0.25d) {
        quartCnt++;
      } else {
        Assertions.fail("Value should not exist: " + val);
      }
    }

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - 2, zeroCnt);
    Assertions.assertEquals(1, quartCnt);
    Assertions.assertEquals(1, eighthCnt);

    peerAppraisal.flushHourlyReportIntake();
    Assertions.assertEquals(0.5d, peerAppraisal.getContractSuccessChance());
  }

  @Test
  void assertCorrectFourWeeksDuplicateConfirmationTest() throws Exception {
    DateTime baseDateTime = DateTime.now();
    PeerAppraisal peerAppraisal = new PeerAppraisal("dummyPeer1", baseDateTime);
    peerAppraisal.registerReport(baseDateTime.plusHours(1), 0, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard1");
    peerAppraisal.registerReport(baseDateTime.plusHours(2), 2, "shard1");
    double[] emptyDistribution = peerAppraisal.getShardReturnDistribution(baseDateTime.plusWeeks(5));

    int zeroCnt = 0;
    int tenthCnt = 0;
    int fifth = 0;

    for (double val : emptyDistribution) {
      if (val == 0.0d) {
        zeroCnt++;
      } else if (val == 0.1d) {
        tenthCnt++;
      } else if (val == 0.2d) {
        fifth++;
      } else {
        Assertions.fail("Value should not exist: " + val);
      }
    }

    Assertions.assertEquals(DateTimeConstants.HOURS_PER_WEEK - 2, zeroCnt);
    Assertions.assertEquals(1, fifth);
    Assertions.assertEquals(1, tenthCnt);

    peerAppraisal.flushHourlyReportIntake();
    Assertions.assertEquals(0.5d, peerAppraisal.getContractSuccessChance());
  }
}