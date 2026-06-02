package com.github.skozlov.async.task;

import com.github.skozlov.async.collection.LinkedQueue;
import com.github.skozlov.async.collection.Try;
import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import com.github.skozlov.async.task.TestWorker.TaskError;
import com.github.skozlov.async.task.TestWorker.WorkerError;
import com.github.skozlov.async.task.TestWorker.WorkerCompletion;
import com.github.skozlov.commons.test.TestingConcurrentEnv;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static com.github.skozlov.commons.test.ThreadTestUtils.withThread;
import static java.lang.Thread.State.TIMED_WAITING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorkerTest {
  private final TestingConcurrentEnv env = new TestingConcurrentEnv();
  private final Clock clock = env.getClock();

  @Test
  void noTasks() throws Exception {
    var worker = new TestWorker(new LinkedQueue<>(1));
    withThread(
        () -> worker.execute(new Deadline(clock.instant().plusSeconds(1), clock)),
        workerThread -> {
          workerThread.start();
          assertEquals(List.of(), worker.events);

          worker.shutDown(true);
          workerThread.join();
        }
    );
    assertEquals(List.of(WorkerCompletion.INSTANCE), worker.events);
  }

  @Test
  void gracefulShutdown() throws Exception {
    var deadline = new Deadline(clock.instant().plusSeconds(1), clock);
    var queue = new LinkedQueue<Task>(1);
    var task = new TestTask(deadline);
    task.pause();
    queue.enqueue(List.of(task), deadline);
    var worker = new TestWorker(queue);
    withThread(
        () -> worker.execute(deadline),
        workerThread -> {
          workerThread.start();
          task.awaitPaused();
          assertEquals(List.of(), worker.events);

          worker.shutDown(false);
          assertEquals(List.of(), worker.events);

          task.resume();
          workerThread.join();
        }
    );
    assertEquals(1, task.getCount());
    assertEquals(List.of(WorkerCompletion.INSTANCE), worker.events);
  }

  @Test
  void forceShutdown() throws Exception {
    var deadline = new Deadline(clock.instant().plusSeconds(1), clock);
    var queue = new LinkedQueue<Task>(1);
    var task = new TestTask(deadline);
    task.pause();
    queue.enqueue(List.of(task), deadline);
    var worker = new TestWorker(queue);
    withThread(
        () -> worker.execute(deadline),
        workerThread -> {
          workerThread.start();
          task.awaitPaused();
          assertEquals(List.of(), worker.events);

          worker.shutDown(true);
          workerThread.join();
        }
    );
    assertEquals(0, task.getCount());
    assertThat(worker.events).hasSize(2);
    assertThat(((TaskError) worker.events.getFirst()).e().getCause()).isInstanceOf(InterruptedException.class);
    assertThat(worker.events.get(1)).isEqualTo(WorkerCompletion.INSTANCE);
  }

  @Test
  void taskDequeueInterrupted() throws Exception {
    var worker = new TestWorker(new LinkedQueue<>(1));
    withThread(
        () -> worker.execute(new Deadline(clock.instant().plusSeconds(1), clock)),
        workerThread -> {
          workerThread.start();
          assertEquals(List.of(), worker.events);

          workerThread.interrupt();
          while (workerThread.getState() != TIMED_WAITING) {
            Thread.yield();
          }
          assertFalse(workerThread.isInterrupted());
          assertThat(worker.events).hasSize(1);
          var workerError = (WorkerError) worker.events.getFirst();
          assertThat(workerError.e().getCause()).isInstanceOf(InterruptedException.class);

          worker.shutDown(true);
          workerThread.join();
          assertThat(worker.events).isEqualTo(List.of(workerError, WorkerCompletion.INSTANCE));
        }
    );
  }

  @Test
  void taskExecutionInterrupted() throws Exception {
    var deadline = new Deadline(clock.instant().plusSeconds(1), clock);
    var queue = new LinkedQueue<Task>(1);
    var task = new TestTask(deadline);
    task.pause();
    queue.enqueue(List.of(task), deadline);
    var worker = new TestWorker(queue);
    withThread(
        () -> worker.execute(deadline),
        workerThread -> {
          workerThread.start();
          task.awaitPaused();
          assertEquals(List.of(), worker.events);

          workerThread.interrupt();
          while (workerThread.getState() != TIMED_WAITING) {
            Thread.yield();
          }
          assertFalse(workerThread.isInterrupted());
          assertThat(worker.events).hasSize(1);
          var taskError = (TaskError) worker.events.getFirst();
          assertThat(taskError.e().getCause()).isInstanceOf(InterruptedException.class);

          worker.shutDown(true);
          workerThread.join();
          assertThat(worker.events).isEqualTo(List.of(taskError, WorkerCompletion.INSTANCE));
        }
    );
    assertEquals(0, task.getCount());
    assertThat(worker.events).hasSize(2);
    assertThat(((TaskError) worker.events.getFirst()).e().getCause()).isInstanceOf(InterruptedException.class);
    assertThat(worker.events.get(1)).isEqualTo(WorkerCompletion.INSTANCE);
  }

  @Test
  void workerExecutionDeadlinePassed() throws Exception {
    var workerDeadline = new Deadline(clock.instant().plusSeconds(1), clock);
    var taskDeadline = new Deadline(clock.instant().plusSeconds(2), clock);
    var queue = new LinkedQueue<Task>(1);
    var task = new TestTask(taskDeadline);
    task.pause();
    queue.enqueue(List.of(task), taskDeadline);
    var worker = new TestWorker(queue);
    withThread(
        () -> worker.execute(workerDeadline),
        workerThread -> {
          workerThread.start();
          task.awaitPaused();
          assertEquals(List.of(), worker.events);

          env.sleep(Duration.ofSeconds(1));
          assertEquals(List.of(), worker.events);

          task.resume();
          workerThread.join();
        }
    );
    assertEquals(1, task.getCount());
    assertThat(worker.events).hasSize(2);
    var cause = ((WorkerError) worker.events.getFirst()).e().getCause();
    assertThat(cause).isInstanceOf(DeadlinePassedException.class);
    assertThat(cause.getMessage()).isEqualTo(workerDeadline + " passed");
    assertThat(worker.events.get(1)).isEqualTo(WorkerCompletion.INSTANCE);
  }

  @Test
  void workerFatalException() throws Exception {
    var queue = new LinkedQueue<Task>(1){
      @Override
      public @NonNull Try<List<Task>> dequeue(int minElements, int maxElements, @NonNull Deadline deadline) {
        throw new UnsupportedOperationException("test");
      }
    };
    var worker = new TestWorker(queue);
    withThread(
        () -> worker.execute(new Deadline(clock.instant().plusSeconds(1), clock)),
        workerThread -> {
          workerThread.start();
          workerThread.join();
        }
    );
    assertThat(worker.events).hasSize(2);
    var cause = ((WorkerError) worker.events.getFirst()).e().getCause();
    assertThat(cause).isInstanceOf(UnsupportedOperationException.class);
    assertThat(cause.getMessage()).isEqualTo("test");
    assertThat(worker.events.get(1)).isEqualTo(WorkerCompletion.INSTANCE);
  }
}
