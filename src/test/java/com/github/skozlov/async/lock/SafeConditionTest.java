package com.github.skozlov.async.lock;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import com.github.skozlov.async.function.InterruptibleSupplier;
import com.github.skozlov.commons.test.TestingConcurrentEnv;
import com.github.skozlov.commons.test.TestingConcurrentEnv.Await;
import com.github.skozlov.commons.test.TestingConcurrentEnv.AwaitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SafeConditionTest {
  private TestingConcurrentEnv env;

  @BeforeEach
  void init() {
    env = new TestingConcurrentEnv();
  }

  @Nested
  class AwaitTest {
    private final long timeoutNanos = 1234;
    private Deadline deadline;

    @BeforeEach
    void init() {
      deadline = new Deadline(env.getClock().instant().plusNanos(timeoutNanos), env.getClock());
    }

    private static InterruptibleSupplier<Boolean> newUntilSupplier(int falseNumber) {
      AtomicInteger falseNumberSoFar = new AtomicInteger(1);
      return () -> !(falseNumberSoFar.getAndIncrement() <= falseNumber);
    }

    @Nested
    class Success {
      @Nested
      class SingleAwait {
        private void check(long awaitNanos) {
          try {
            Condition unsafeMock = env.newConditionMock(singleton(
                new Await(Duration.ofNanos(awaitNanos), AwaitResult.SIGNAL)
            ));
            SafeCondition TOT = SafeCondition.from(unsafeMock);
            TOT.await(deadline, newUntilSupplier(1));
            verify(unsafeMock, times(1)).await(eq(timeoutNanos), eq(NANOSECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }

        @Test
        void signalBeforeDeadline() {
          check(timeoutNanos - 1);
        }

        @Test
        void signalAtDeadline() {
          check(timeoutNanos);
        }

        @Test
        void signalAfterDeadline() {
          check(timeoutNanos + 1);
        }
      }

      @Nested
      class SpuriousWakeup {
        private void check(long totalAwaitNanos) {
          try {
            long firstAwaitNanos = timeoutNanos / 2;
            long secondAwaitNanos = totalAwaitNanos - firstAwaitNanos;
            Condition unsafeMock = env.newConditionMock(Arrays.asList(
                new Await(Duration.ofNanos(firstAwaitNanos), AwaitResult.SIGNAL),
                new Await(Duration.ofNanos(secondAwaitNanos), AwaitResult.SIGNAL)
            ));
            SafeCondition TOT = SafeCondition.from(unsafeMock);
            TOT.await(deadline, newUntilSupplier(2));
            verify(unsafeMock, times(1)).await(eq(firstAwaitNanos), eq(NANOSECONDS));
            verify(unsafeMock, times(1)).await(eq(timeoutNanos - firstAwaitNanos), eq(NANOSECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }

        @Test
        void trueSignalBeforeDeadline() {
          check(timeoutNanos - 1);
        }

        @Test
        void trueSignalAtDeadline() {
          check(timeoutNanos);
        }

        @Test
        void trueSignalAfterDeadline() {
          check(timeoutNanos + 1);
        }
      }

      @Nested
      class TimedOutButUntilIsSatisfied {
        @Test
        void beforeDeadline() {
          try {
            Condition unsafeMock = env.newConditionMock(singleton(
                new Await(Duration.ofNanos(timeoutNanos - 1), AwaitResult.TIMEOUT)
            ));
            SafeCondition TOT = SafeCondition.from(unsafeMock);
            TOT.await(deadline, newUntilSupplier(1));
            verify(unsafeMock, times(1)).await(eq(timeoutNanos), eq(NANOSECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }

        @Test
        void atDeadline() {
          try {
            Condition unsafeMock = env.newConditionMock(singleton(
                new Await(Duration.ofNanos(timeoutNanos), AwaitResult.TIMEOUT)
            ));
            SafeCondition TOT = SafeCondition.from(unsafeMock);
            TOT.await(deadline, newUntilSupplier(1));
            verify(unsafeMock, times(1)).await(eq(timeoutNanos), eq(NANOSECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }

        @Test
        void afterDeadlineSingleAwait() {
          try {
            Condition unsafeMock = env.newConditionMock(singleton(
                new Await(Duration.ofNanos(timeoutNanos + 1), AwaitResult.TIMEOUT)
            ));
            SafeCondition TOT = SafeCondition.from(unsafeMock);
            TOT.await(deadline, newUntilSupplier(1));
            verify(unsafeMock, times(1)).await(eq(timeoutNanos), eq(NANOSECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }

        @Test
        void afterDeadlineMultipleAwaits() {
          try {
            long awaitNanos = timeoutNanos - 1;
            Condition unsafeMock = env.newConditionMock(Arrays.asList(
                new Await(Duration.ofNanos(awaitNanos), AwaitResult.TIMEOUT),
                new Await(Duration.ofNanos(awaitNanos), AwaitResult.TIMEOUT)
            ));
            SafeCondition TOT = SafeCondition.from(unsafeMock);
            TOT.await(deadline, newUntilSupplier(2));
            verify(unsafeMock, times(1)).await(eq(timeoutNanos), eq(NANOSECONDS));
            verify(unsafeMock, times(1)).await(eq(timeoutNanos - awaitNanos), eq(NANOSECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }
      }
    }

    @Nested
    class TimedOut {
      private void check(long awaitNanos) {
        try {
          Condition unsafeMock = env.newConditionMock(singleton(
              new Await(Duration.ofNanos(awaitNanos), AwaitResult.TIMEOUT)
          ));
          SafeCondition TOT = SafeCondition.from(unsafeMock);
          assertThatThrownBy(() -> TOT.await(deadline, newUntilSupplier(2)))
              .isInstanceOf(DeadlinePassedException.class)
              .hasMessage("Deadline(1970-01-01T00:00:00.000001234Z) passed");
          verify(unsafeMock, times(1)).await(eq(timeoutNanos), eq(NANOSECONDS));
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
    void interruptedTest() throws InterruptedException {
      Condition unsafeMock = env.newConditionMock(singleton(
          new Await(Duration.ofNanos(timeoutNanos), AwaitResult.INTERRUPT)
      ));
      SafeCondition TOT = SafeCondition.from(unsafeMock);
      assertThatThrownBy(() -> TOT.await(deadline, newUntilSupplier(1)))
          .isInstanceOf(InterruptedException.class);
      verify(unsafeMock, times(1)).await(eq(timeoutNanos), eq(NANOSECONDS));
    }
  }

  @Test
  void signalTest() {
    Condition unsafeMock = env.newConditionMock(emptyList());
    SafeCondition.from(unsafeMock).signal();
    verify(unsafeMock, times(1)).signal();
  }

  @Test
  void signalAllTest() {
    Condition unsafeMock = env.newConditionMock(emptyList());
    SafeCondition.from(unsafeMock).signalAll();
    verify(unsafeMock, times(1)).signalAll();
  }
}
