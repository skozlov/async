package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import com.github.skozlov.async.function.PartialResult;
import com.github.skozlov.commons.Pair;
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
    switch (TOT.enqueue(Arrays.asList(elements), deadline)) {
      case PartialResult.Success<Pair<Integer, Iterator<? extends Integer>>> s -> {
        var result = s.result();
        assertEquals(elements.length, result._1());
        assertFalse(result._2().hasNext());
      }
      case PartialResult.DeadlinePassed<?> p -> throw p.exception();
      case PartialResult.Interrupted<?> i -> throw new RuntimeException(i.exception());
    }
  }

  private List<Integer> dequeueSuccessfully(Deadline deadline, int minElements, int maxElements) {
    return switch (TOT.dequeue(minElements, maxElements, deadline)) {
      case PartialResult.Success<List<Integer>> s -> s.result();
      case PartialResult.DeadlinePassed<?> p -> throw p.exception();
      case PartialResult.Interrupted<?> i -> throw new RuntimeException(i.exception());
    };
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
      switch (TOT.enqueue(List.of(10, 20, 30, 40), deadline)) {
        case PartialResult.DeadlinePassed<Pair<Integer, Iterator<? extends Integer>>> p -> {
          assertInstanceOf(DeadlinePassedException.class, p.exception());
          assertEquals("Deadline(1970-01-01T00:00:00Z) passed", p.exception().getMessage());

          assertEquals(3, p.partialResult()._1());

          Iterator<? extends Integer> notInsertedElements = p.partialResult()._2();
          assertEquals(40, notInsertedElements.next());
          assertFalse(notInsertedElements.hasNext());
        }
        case PartialResult.Success<?> s -> fail("Unexpected successful result: " + s);
        case PartialResult.Interrupted<?> i -> throw new RuntimeException(i.exception());
      }
      assertEquals(List.of(10, 20, 30), dequeueSuccessfully(deadline, 0, 3));
    }

    @Test
    void dequeue() {
      enqueueSuccessfully(deadline, 10, 20);
      switch (TOT.dequeue(3, 3, deadline)) {
        case PartialResult.DeadlinePassed<List<Integer>> p -> {
          assertInstanceOf(DeadlinePassedException.class, p.exception());
          assertEquals("Deadline(1970-01-01T00:00:00Z) passed", p.exception().getMessage());
          assertEquals(List.of(10, 20), p.partialResult());
        }
        case PartialResult.Success<?> s -> fail("Unexpected successful result: " + s);
        case PartialResult.Interrupted<?> i -> throw new RuntimeException(i.exception());
      }
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
            switch (TOT.enqueue(List.of(10, 20, 30, 40), deadline)) {
              case PartialResult.Interrupted<Pair<Integer, Iterator<? extends Integer>>> i -> {
                assertNotNull(i.exception());
                assertEquals(3, i.partialResult()._1());
                assertEquals(40, i.partialResult()._2().next());
              }
              case PartialResult.Success<?> s -> fail("Unexpected successful result: " + s);
              case PartialResult.DeadlinePassed<?> p -> throw p.exception();
            }
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
            switch (TOT.dequeue(3, 3, deadline)) {
              case PartialResult.Interrupted<List<Integer>> i -> {
                assertNotNull(i.exception());
                assertEquals(List.of(10, 20), i.partialResult());
              }
              case PartialResult.Success<?> s -> fail("Unexpected successful result: " + s);
              case PartialResult.DeadlinePassed<?> p -> throw p.exception();
            }
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
