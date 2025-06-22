package com.github.skozlov.commons.test;

import lombok.Getter;
import lombok.NonNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

import static java.time.ZoneOffset.UTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestingConcurrentEnv {
  @Getter
  private final Instant initTime = Instant.EPOCH;
  private final AtomicReference<Instant> now = new AtomicReference<>(initTime);
  @Getter
  private final Clock clock = new Clock() {
    @Override
    public ZoneId getZone() {
      return UTC;
    }

    @Override
    public Clock withZone(ZoneId zoneId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  };

  public @NonNull Condition newConditionMock(@NonNull Iterable<Await> awaits) {
    try {
      Iterator<Await> it = awaits.iterator();
      var condition = mock(Condition.class);
      var index = new AtomicInteger(0);
      when(condition.await(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
        int i = index.getAndIncrement();
        if (!it.hasNext()) {
          throw new NoSuchElementException("No more awaits for this mock, there are only " + i);
        }
        Await await = it.next();
        now.getAndUpdate(n -> n.plus(await.duration.toNanos(), ChronoUnit.NANOS));
        return switch (await.result) {
          case SIGNAL -> true;
          case TIMEOUT -> false;
          case INTERRUPT -> throw new InterruptedException();
        };
      });
      return condition;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  public enum AwaitResult {
    SIGNAL, TIMEOUT, INTERRUPT
  }

  public record Await(@NonNull Duration duration, @NonNull AwaitResult result) {
  }
}
