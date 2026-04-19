package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.function.PartialResult;
import com.github.skozlov.commons.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.github.skozlov.commons.test.ThreadTestUtils.withThread;
import static java.lang.Thread.State.TIMED_WAITING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LinkedQueueWithDeadlineTest {
  private final LinkedQueueWithDeadline<Integer> TOT = new LinkedQueueWithDeadline<>(3);

  @Nested
  class Success {
    private final Clock clock = Clock.systemUTC();
    private final Deadline deadline = new Deadline(clock.instant().plus(1, ChronoUnit.HOURS), clock);

    private void enqueue(Integer... elements) {
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

    private List<Integer> dequeue(int minElements, int maxElements) {
      return switch (TOT.dequeue(minElements, maxElements, deadline)) {
        case PartialResult.Success<List<Integer>> s -> s.result();
        case PartialResult.DeadlinePassed<?> p -> throw p.exception();
        case PartialResult.Interrupted<?> i -> throw new RuntimeException(i.exception());
      };
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

          assertEquals(List.of(1), dequeue(1, 1));
          enqueueThread.join();

          assertEquals(List.of(2, 3, 4), dequeue(0, 3));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
