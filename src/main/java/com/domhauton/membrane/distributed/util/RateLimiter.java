package com.domhauton.membrane.distributed.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Dominic Hauton on 04/03/17.
 */
public class RateLimiter {
  private final Logger logger = LogManager.getLogger();

  private final AtomicLong triggerCount;
  private final Runnable uploadRunnable;
  private final Duration runLag;

  /**
   * @param uploadRunnable
   * @param runLag
   */
  public RateLimiter(Runnable uploadRunnable, Duration runLag) {
    this.uploadRunnable = uploadRunnable;
    this.runLag = runLag;

    triggerCount = new AtomicLong(0L);
  }

  public void schedule() {
    long previousCount = triggerCount.getAndIncrement();
    if (previousCount == 0) {
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(runLag.getMillis());
          long requests = triggerCount.getAndSet(0);
          logger.info("Running upload with {} requests", requests);
          uploadRunnable.run();
        } catch (InterruptedException e) {
          // If thread has been interrupted short circuit to end.
          long requests = triggerCount.getAndSet(0);
          logger.warn("Upload Rate Limiter sleep interrupted with {} requests. Skipping upload.", requests);
        }
      });
    }
  }
}
