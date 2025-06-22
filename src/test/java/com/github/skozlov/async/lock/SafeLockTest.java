package com.github.skozlov.async.lock;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import com.github.skozlov.commons.test.TestingConcurrentEnv;
import com.github.skozlov.commons.test.TestingConcurrentEnv.TryLock;
import com.github.skozlov.commons.test.TestingConcurrentEnv.TryLockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SafeLockTest {
  private TestingConcurrentEnv env;

  @BeforeEach
  void init() {
    env = new TestingConcurrentEnv();
  }

  @Nested
  class LockTest {
    private final long timeoutNanos = 1234;
    private Deadline deadline;

    @BeforeEach
    void init() {
      deadline = new Deadline(env.getClock().instant().plusNanos(timeoutNanos), env.getClock());
    }

    @Nested
    class Acquired {
      @Test
      void startBeforeDeadline() throws InterruptedException {
        long toDeadlineNanos = 1;
        env.sleep(Duration.ofNanos(timeoutNanos - toDeadlineNanos));
        Lock unsafeMock = env.newLockMock(singleton(
            new TryLock(Duration.ofNanos(toDeadlineNanos), TryLockResult.ACQUIRED)
        ));
        try (var ignored = SafeLock.from(unsafeMock).lock(deadline)) {
          verify(unsafeMock, times(1)).tryLock(eq(toDeadlineNanos), eq(NANOSECONDS));
        }
        verify(unsafeMock, times(1)).unlock();
      }

      @Test
      void startAtDeadline() throws InterruptedException {
        env.sleep(Duration.ofNanos(timeoutNanos));
        Lock unsafeMock = env.newLockMock(singleton(
            new TryLock(Duration.ZERO, TryLockResult.ACQUIRED)
        ));
        try (var ignored = SafeLock.from(unsafeMock).lock(deadline)) {
          verify(unsafeMock, times(1)).tryLock(eq(0L), eq(NANOSECONDS));
        }
        verify(unsafeMock, times(1)).unlock();
      }

      @Test
      void startAfterDeadline() throws InterruptedException {
        env.sleep(Duration.ofNanos(timeoutNanos + 1));
        Lock unsafeMock = env.newLockMock(singleton(
            new TryLock(Duration.ZERO, TryLockResult.ACQUIRED)
        ));
        try (var ignored = SafeLock.from(unsafeMock).lock(deadline)) {
          verify(unsafeMock, times(1)).tryLock(eq(0L), eq(NANOSECONDS));
        }
        verify(unsafeMock, times(1)).unlock();
      }

      @Test
      void multipleTimes() throws InterruptedException {
        Lock unsafeMock = env.newLockMock(Arrays.asList(
            new TryLock(Duration.ofNanos(timeoutNanos - 1), TryLockResult.TIMEOUT),
            new TryLock(Duration.ofNanos(2), TryLockResult.ACQUIRED)
        ));
        try (var ignored = SafeLock.from(unsafeMock).lock(deadline)) {
          verify(unsafeMock, times(1)).tryLock(eq(timeoutNanos), eq(NANOSECONDS));
          verify(unsafeMock, times(1)).tryLock(eq(1L), eq(NANOSECONDS));
        }
        verify(unsafeMock, times(1)).unlock();
      }
    }

    @Nested
    class TimedOut {
      private void check(long tryLockNanos) {
        try {
          Lock unsafeMock = env.newLockMock(Arrays.asList(
              new TryLock(Duration.ofNanos(tryLockNanos), TryLockResult.TIMEOUT),
              new TryLock(Duration.ZERO, TryLockResult.TIMEOUT)
          ));
          SafeLock TOT = SafeLock.from(unsafeMock);
          //noinspection resource
          assertThatThrownBy(() -> TOT.lock(deadline))
              .isInstanceOf(DeadlinePassedException.class)
              .hasMessage("Deadline(1970-01-01T00:00:00.000001234Z) passed");
          verify(unsafeMock, times(1)).tryLock(timeoutNanos, NANOSECONDS);
          verify(unsafeMock, times(0)).unlock();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      }

      @Test
      void atDeadline() {
        check(timeoutNanos);
      }

      @Test
      void afterDeadline() {
        check(timeoutNanos + 1);
      }
    }

    @Test
    void interruptionTest() throws InterruptedException {
      Lock unsafeMock = env.newLockMock(singleton(new TryLock(Duration.ofNanos(1), TryLockResult.INTERRUPT)));
      SafeLock TOT = SafeLock.from(unsafeMock);
      //noinspection resource
      assertThatThrownBy(() -> TOT.lock(deadline)).isInstanceOf(InterruptedException.class);
      verify(unsafeMock, times(1)).tryLock(timeoutNanos, NANOSECONDS);
      verify(unsafeMock, times(0)).unlock();
    }
  }

  @Test
  void newConditionTest() {
    Condition conditionMock = mock(Condition.class);
    Lock unsafeMock = mock(Lock.class);
    when(unsafeMock.newCondition()).thenReturn(conditionMock);
    assertThat(SafeLock.from(unsafeMock).newCondition().toUnsafe()).isSameAs(conditionMock);
  }
}
