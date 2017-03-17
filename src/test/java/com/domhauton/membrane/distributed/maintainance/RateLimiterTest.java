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
class RateLimiterTest {

  @Test
  void testRateLimiting() throws Exception {
    Runnable runnableMock = Mockito.mock(Runnable.class);

    RateLimiter uploadRateLimiter = new RateLimiter(runnableMock, Duration.millis(200));

    ScheduledExecutorService addExecutor = Executors.newSingleThreadScheduledExecutor();
    ScheduledFuture<?> looper = addExecutor.scheduleWithFixedDelay(uploadRateLimiter::schedule, 0, 10, TimeUnit.MILLISECONDS);

    addExecutor.schedule(() -> Mockito.verify(runnableMock, Mockito.times(1)).run(), 300, TimeUnit.MILLISECONDS);
    addExecutor.schedule(() -> {
      looper.cancel(true);
      Mockito.verify(runnableMock, Mockito.atMost(2)).run();
    }, 500, TimeUnit.MILLISECONDS);
    ScheduledFuture<?> testEnd = addExecutor.schedule(() -> Mockito.verify(runnableMock, Mockito.times(3)).run(), 850, TimeUnit.MILLISECONDS);

    testEnd.get(1, TimeUnit.SECONDS);
  }
}