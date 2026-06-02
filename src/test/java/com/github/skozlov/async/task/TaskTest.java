package com.github.skozlov.async.task;

import com.github.skozlov.async.cancel.CancelException;
import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import com.github.skozlov.commons.test.TestingConcurrentEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Duration;

import static com.github.skozlov.commons.test.ThreadTestUtils.withThread;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
  private final TestingConcurrentEnv env = new TestingConcurrentEnv();
  private final Clock clock = env.getClock();

  @Test
  void success() throws Exception {
    var deadline = new Deadline(clock.instant().plusSeconds(1), clock);
    var task = new TestTask(deadline);
    task.execute();
    assertEquals(1, task.getCount());
  }

  @Test
  void deadlinePassedBeforeStart() {
    var deadline = new Deadline(clock.instant(), clock);
    var task = new TestTask(deadline);
    assertEquals(
        deadline + " passed",
        assertThrows(DeadlinePassedException.class, task::execute).getMessage()
    );
  }

  @Test
  void deadlinePassedAfterStart() throws Exception {
    var deadline = new Deadline(clock.instant().plusSeconds(1), clock);
    var task = new TestTask(deadline){
      @Override
      protected void executeImpl() throws InterruptedException {
        env.sleep(Duration.ofSeconds(1));
        assertEquals(deadline.toInstant(), clock.instant());
        super.executeImpl();
      }
    };
    task.execute();
    assertEquals(1, task.getCount());
  }

  @ParameterizedTest
  @CsvSource({"false", "true"})
  void cancelBeforeStart(boolean interrupt) {
    var task = new TestTask(new Deadline(clock.instant().plusSeconds(1), clock));
    task.cancel(interrupt);
    assertNull(assertThrows(CancelException.class, task::execute).getMessage());
    assertEquals(0, task.getCount());
    assertFalse(Thread.currentThread().isInterrupted());
  }

  @Test
  void cancelAfterStartNoInterruption() throws Exception {
    var task = new TestTask(new Deadline(clock.instant().plusSeconds(1), clock));
    withThread(
        task::execute,
        thread -> {
          task.pause();
          thread.start();
          task.awaitPaused();
          task.cancel(false);
          task.resume();
          thread.join();
          assertFalse(thread.isInterrupted());
        }
    );
    assertEquals(1, task.getCount());
  }

  @Test
  void cancelAfterStartWithInterruption() throws Exception {
    var task = new TestTask(new Deadline(clock.instant().plusSeconds(1), clock));
    withThread(
        () -> {
          var e = assertThrows(CancelException.class, task::execute);
          assertInstanceOf(InterruptedException.class, e.getCause());
        },
        thread -> {
          task.pause();
          thread.start();
          task.awaitPaused();
          task.cancel(true);
          thread.join();
          assertFalse(thread.isInterrupted());
        }
    );
    assertEquals(0, task.getCount());
  }

  @Test
  void interruptBeforeStart() {
    var task = new TestTask(new Deadline(clock.instant().plusSeconds(1), clock));
    Thread.currentThread().interrupt();
    assertNull(assertThrows(InterruptedException.class, task::execute).getMessage());
    assertEquals(0, task.getCount());
  }

  @Test
  void interruptAfterStart() throws Exception {
    var task = new TestTask(new Deadline(clock.instant().plusSeconds(1), clock));
    withThread(
        () -> assertThrows(InterruptedException.class, task::execute),
        thread -> {
          task.pause();
          thread.start();
          task.awaitPaused();
          thread.interrupt();
          thread.join();
        }
    );
    assertEquals(0, task.getCount());
  }
}
