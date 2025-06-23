package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import lombok.NonNull;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public interface QueueWithDeadline<E> {
  @NonNull BlockingQueue<E> getWrappedBlockingQueue();

  default void enqueue(@NonNull E e, @NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException {
    var blockingQueue = getWrappedBlockingQueue();
    for (;;) {
      Duration timeout = deadline.getTimeLeft();
      boolean added = blockingQueue.offer(e, timeout.toNanos(), NANOSECONDS);
      if (added) {
        return;
      } else if (!timeout.isPositive()) {
        throw new DeadlinePassedException(deadline);
      }
    }
  }

  default @NonNull E dequeue(@NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException {
    var blockingQueue = getWrappedBlockingQueue();
    for (;;) {
      Duration timeout = deadline.getTimeLeft();
      E e = blockingQueue.poll(timeout.toNanos(), NANOSECONDS);
      if (e != null) {
        return e;
      } else if (!timeout.isPositive()) {
        throw new DeadlinePassedException(deadline);
      }
    }
  }
}
