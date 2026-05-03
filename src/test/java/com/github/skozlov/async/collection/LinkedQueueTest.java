package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.github.skozlov.commons.test.ThreadTestUtils.withThread;
import static java.lang.Thread.State.TIMED_WAITING;
import static org.junit.jupiter.api.Assertions.*;

class LinkedQueueTest {
  private final LinkedQueue<Integer> TOT = new LinkedQueue<>(3);

  private void enqueueSuccessfully(Deadline deadline, Integer... elements) {
    var result = TOT.enqueue(Arrays.asList(elements), deadline).asSuccess();
    assertEquals(elements.length, result._1());
    assertFalse(result._2().hasNext());
  }

  private List<Integer> dequeueSuccessfully(Deadline deadline, int minElements, int maxElements) {
    return TOT.dequeue(minElements, maxElements, deadline).asSuccess();
  }

  @Nested
  class Success {
    private final Clock clock = Clock.systemUTC();
    private final Deadline deadline = new Deadline(clock.instant().plus(1, ChronoUnit.HOURS), clock);

    private void enqueue(Integer... elements) {
      enqueueSuccessfully(deadline, elements);
    }

    private List<Integer> dequeue(int minElements, int maxElements) {
      return dequeueSuccessfully(deadline, minElements, maxElements);
    }

    @Test
    void enqueueWithoutBlocking() {
      enqueue();
      enqueue(1);
      enqueue(2, 3);
    }

    @Test
    void dequeueWithoutBlocking() {
      assertEquals(List.of(), dequeue(0, 0));

      enqueue(1, 2, 3);
      assertEquals(List.of(), dequeue(0, 0));
      assertEquals(List.of(1), dequeue(0, 1));
      assertEquals(List.of(2, 3), dequeue(0, 2));
    }

    @Test
    void enqueueWithBlocking() {
      enqueue(1, 2);
      withThread(() -> enqueue(3, 4), enqueueThread -> {
        try {
          enqueueThread.start();
          while (enqueueThread.getState() != TIMED_WAITING) {
            Thread.yield();
          }

          assertEquals(List.of(1), dequeue(0, 1));
          enqueueThread.join();

          assertEquals(List.of(2, 3, 4), dequeue(0, 3));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
    }

    @Test
    void dequeueWithBlocking() {
      enqueue(1);
      withThread(
          () -> assertEquals(List.of(1, 2), dequeue(2, 3)),
          dequeueThread -> {
            try {
              dequeueThread.start();
              while (dequeueThread.getState() != TIMED_WAITING) {
                Thread.yield();
              }

              enqueue(2);
              dequeueThread.join();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
      );
    }
  }

  @Nested
  class DeadlinePassed {
    private final Clock clock = Clock.systemUTC();
    private final Deadline deadline = new Deadline(Instant.EPOCH, clock);

    @Test
    void enqueue() {
      var result = TOT.enqueue(List.of(10, 20, 30, 40), deadline).asDeadlinePassed();
      assertInstanceOf(DeadlinePassedException.class, result.exception());
      assertEquals("Deadline(1970-01-01T00:00:00Z) passed", result.exception().getMessage());

      assertEquals(3, result.partialResult()._1());

      Iterator<? extends Integer> notInsertedElements = result.partialResult()._2();
      assertEquals(40, notInsertedElements.next());
      assertFalse(notInsertedElements.hasNext());
    }

    @Test
    void dequeue() {
      enqueueSuccessfully(deadline, 10, 20);
      var result = TOT.dequeue(3, 3, deadline).asDeadlinePassed();
      assertInstanceOf(DeadlinePassedException.class, result.exception());
      assertEquals("Deadline(1970-01-01T00:00:00Z) passed", result.exception().getMessage());
      assertEquals(List.of(10, 20), result.partialResult());
    }
  }

  @Nested
  class Interrupted {
    private final Clock clock = Clock.systemUTC();
    private final Deadline deadline = new Deadline(clock.instant().plus(1, ChronoUnit.HOURS), clock);

    @Test
    void enqueue() {
      withThread(
          () -> {
            var result = TOT.enqueue(List.of(10, 20, 30, 40), deadline).asInterrupted();
            assertNotNull(result.exception());
            assertEquals(3, result.partialResult()._1());
            assertEquals(40, result.partialResult()._2().next());
          },
          enqueueThread -> {
            try {
              enqueueThread.start();
              while (enqueueThread.getState() != TIMED_WAITING) {
                Thread.yield();
              }
              enqueueThread.interrupt();
              enqueueThread.join();
              assertTrue(enqueueThread.isInterrupted());
              assertEquals(List.of(10, 20, 30), dequeueSuccessfully(deadline, 0, 3));
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
      );
    }

    @Test
    void dequeue() {
      enqueueSuccessfully(deadline, 10, 20);
      withThread(
          () -> {
            var i = TOT.dequeue(3, 3, deadline).asInterrupted();
            assertNotNull(i.exception());
            assertEquals(List.of(10, 20), i.partialResult());
          },
          dequeueThread -> {
            try {
              dequeueThread.start();
              while (dequeueThread.getState() != TIMED_WAITING) {
                Thread.yield();
              }
              dequeueThread.interrupt();
              dequeueThread.join();
              assertTrue(dequeueThread.isInterrupted());
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
      );
    }
  }
}
