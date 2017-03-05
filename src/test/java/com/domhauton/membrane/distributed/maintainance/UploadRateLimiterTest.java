package com.domhauton.membrane.distributed.maintainance;

import org.joda.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dominic Hauton on 04/03/17.
 */
class UploadRateLimiterTest {

  @Test
  void testRateLimiting() throws Exception {
    Runnable runnableMock = Mockito.mock(Runnable.class);

    UploadRateLimiter uploadRateLimiter = new UploadRateLimiter(runnableMock, Duration.millis(100));

    ScheduledExecutorService addExecutor = Executors.newSingleThreadScheduledExecutor();
    ScheduledFuture<?> looper = addExecutor.scheduleWithFixedDelay(uploadRateLimiter::requestUpload, 0, 10, TimeUnit.MILLISECONDS);

    addExecutor.schedule(() -> Mockito.verify(runnableMock, Mockito.times(4)).run(), 420, TimeUnit.MILLISECONDS);
    addExecutor.schedule(() -> {
      looper.cancel(true);
      Mockito.verify(runnableMock, Mockito.atMost(5)).run();
    }, 570, TimeUnit.MILLISECONDS);
    ScheduledFuture<?> testEnd = addExecutor.schedule(() -> Mockito.verify(runnableMock, Mockito.times(6)).run(), 790, TimeUnit.MILLISECONDS);

    testEnd.get(1, TimeUnit.SECONDS);
  }
}