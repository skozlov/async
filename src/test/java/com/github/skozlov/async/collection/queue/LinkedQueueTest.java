package com.github.skozlov.async.collection.queue;

import com.github.skozlov.async.collection.queue.TestQueueListener.OnDequeuedElements;
import com.github.skozlov.async.collection.queue.TestQueueListener.OnEnqueuedElements;
import com.github.skozlov.async.collection.queue.TestQueueListener.OnWaitingForElements;
import com.github.skozlov.async.collection.queue.TestQueueListener.OnWaitingForFreeSpace;
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
import static org.assertj.core.api.Assertions.assertThat;

class LinkedQueueTest {
  private final TestQueueListener listener = new TestQueueListener();
  private final LinkedQueue<Integer> TOT = new LinkedQueue<>(3, listener);

  private void enqueueSuccessfully(Deadline deadline, Integer... elements) {
    var result = TOT.enqueue(Arrays.asList(elements), deadline).asSuccessOrThrow();
    assertThat(result._1()).isEqualTo(elements.length);
    assertThat(result._2().hasNext()).isFalse();
  }

  private List<Integer> dequeueSuccessfully(Deadline deadline, int minElements, int maxElements) {
    return TOT.dequeue(minElements, maxElements, deadline).asSuccessOrThrow();
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
      assertThat(listener.events).isEqualTo(List.of());

      enqueue(1);
      assertThat(listener.events).isEqualTo(List.of(new OnEnqueuedElements(1, 1)));
      listener.events.clear();

      enqueue(2, 3);
      assertThat(listener.events).isEqualTo(List.of(new OnEnqueuedElements(2, 3)));
    }

    @Test
    void dequeueWithoutBlocking() {
      assertThat(dequeue(0, 0)).isEqualTo(List.of());
      assertThat(listener.events).isEqualTo(List.of());

      enqueue(1, 2, 3);
      listener.events.clear();

      assertThat(dequeue(0, 0)).isEqualTo(List.of());
      assertThat(listener.events).isEqualTo(List.of());

      assertThat(dequeue(0, 1)).isEqualTo(List.of(1));
      assertThat(listener.events).isEqualTo(List.of(new OnDequeuedElements(1, 2)));
      listener.events.clear();

      assertThat(dequeue(0, 2)).isEqualTo(List.of(2, 3));
      assertThat(listener.events).isEqualTo(List.of(new OnDequeuedElements(2, 0)));
    }

    @Test
    void enqueueWithBlocking() throws Exception {
      enqueue(1, 2);
      listener.events.clear();

      withThread(() -> enqueue(3, 4), enqueueThread -> {
        enqueueThread.start();
        while (enqueueThread.getState() != TIMED_WAITING) {
          Thread.yield();
        }
        assertThat(listener.events).isEqualTo(List.of(
            new OnEnqueuedElements(1, 3),
            new OnWaitingForFreeSpace(deadline)
        ));
        listener.events.clear();

        assertThat(dequeue(0, 1)).isEqualTo(List.of(1));
        enqueueThread.join();
        assertThat(listener.events).isEqualTo(List.of(
            new OnDequeuedElements(1, 2),
            new OnEnqueuedElements(1, 3)
        ));

        assertThat(dequeue(0, 3)).isEqualTo(List.of(2, 3, 4));
      });
    }

    @Test
    void dequeueWithBlocking() throws Exception {
      enqueue(1);
      listener.events.clear();

      withThread(
          () -> assertThat(dequeue(2, 3)).isEqualTo(List.of(1, 2)),
          dequeueThread -> {
            dequeueThread.start();
            while (dequeueThread.getState() != TIMED_WAITING) {
              Thread.yield();
            }
            assertThat(listener.events).isEqualTo(List.of(
                new OnDequeuedElements(1, 0),
                new OnWaitingForElements(deadline)
            ));
            listener.events.clear();

            enqueue(2);
            dequeueThread.join();
            assertThat(listener.events).isEqualTo(List.of(
                new OnEnqueuedElements(1, 1),
                new OnDequeuedElements(1, 0)
            ));
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
      var result = TOT.enqueue(List.of(10, 20, 30, 40), deadline).asFailureOrThrow();
      assertThat(result.exception()).isInstanceOf(DeadlinePassedException.class);
      assertThat(result.exception().getMessage()).isEqualTo("Deadline(1970-01-01T00:00:00Z) passed");

      assertThat(result.partialResult()._1()).isEqualTo(3);

      Iterator<? extends Integer> notInsertedElements = result.partialResult()._2();
      assertThat(notInsertedElements.next()).isEqualTo(40);
      assertThat(notInsertedElements.hasNext()).isFalse();

      assertThat(listener.events).isEqualTo(List.of(
          new OnEnqueuedElements(3, 3),
          new OnWaitingForFreeSpace(deadline)
      ));
    }

    @Test
    void dequeue() {
      enqueueSuccessfully(deadline, 10, 20);
      listener.events.clear();

      var result = TOT.dequeue(3, 3, deadline).asFailureOrThrow();
      assertThat(result.exception()).isInstanceOf(DeadlinePassedException.class);
      assertThat(result.exception().getMessage()).isEqualTo("Deadline(1970-01-01T00:00:00Z) passed");
      assertThat(result.partialResult()).isEqualTo(List.of(10, 20));
      assertThat(listener.events).isEqualTo(List.of(
          new OnDequeuedElements(2, 0),
          new OnWaitingForElements(deadline)
      ));
    }
  }

  @Nested
  class Interrupted {
    private final Clock clock = Clock.systemUTC();
    private final Deadline deadline = new Deadline(clock.instant().plus(1, ChronoUnit.HOURS), clock);

    @Test
    void enqueue() throws Exception {
      withThread(
          () -> {
            var result = TOT.enqueue(List.of(10, 20, 30, 40), deadline).asFailureOrThrow();
            assertThat(result.exception()).isInstanceOf(InterruptedException.class);
            assertThat(result.partialResult()._1()).isEqualTo(3);
            assertThat(result.partialResult()._2().next()).isEqualTo(40);
          },
          enqueueThread -> {
            enqueueThread.start();
            while (enqueueThread.getState() != TIMED_WAITING) {
              Thread.yield();
            }
            assertThat(listener.events).isEqualTo(List.of(
                new OnEnqueuedElements(3, 3),
                new OnWaitingForFreeSpace(deadline)
            ));
            listener.events.clear();

            enqueueThread.interrupt();
            enqueueThread.join();
            assertThat(enqueueThread.isInterrupted()).isFalse();
            assertThat(listener.events).isEqualTo(List.of());
            listener.events.clear();

            assertThat(dequeueSuccessfully(deadline, 0, 3)).isEqualTo(List.of(10, 20, 30));
            assertThat(listener.events).isEqualTo(List.of(new OnDequeuedElements(3, 0)));
          }
      );
    }

    @Test
    void dequeue() throws Exception {
      enqueueSuccessfully(deadline, 10, 20);
      listener.events.clear();
      withThread(
          () -> {
            var i = TOT.dequeue(3, 3, deadline).asFailureOrThrow();
            assertThat(i.exception()).isInstanceOf(InterruptedException.class);
            assertThat(i.partialResult()).isEqualTo(List.of(10, 20));
          },
          dequeueThread -> {
            dequeueThread.start();
            while (dequeueThread.getState() != TIMED_WAITING) {
              Thread.yield();
            }
            assertThat(listener.events).isEqualTo(List.of(
                new OnDequeuedElements(2, 0),
                new OnWaitingForElements(deadline)
            ));
            listener.events.clear();

            dequeueThread.interrupt();
            dequeueThread.join();
            assertThat(dequeueThread.isInterrupted()).isFalse();
            assertThat(listener.events).isEqualTo(List.of());
          }
      );
    }
  }
}
