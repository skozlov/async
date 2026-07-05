package com.github.skozlov.async.collection.queue;

import com.github.skozlov.async.deadline.Deadline;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class NonThrowingQueueListener implements QueueListener {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @NonNull
  private final QueueListener delegate;

  protected NonThrowingQueueListener(@NonNull QueueListener delegate) {
    this.delegate = delegate;
  }

  public static @NonNull NonThrowingQueueListener from(@NonNull QueueListener delegate) {
    return switch (delegate) {
      case NonThrowingQueueListener ntql -> ntql;
      default -> new NonThrowingQueueListener(delegate);
    };
  }

  @Override
  public void onWaitingForFreeSpace(@NonNull Deadline deadline) {
    handle(() -> delegate.onWaitingForFreeSpace(deadline));
  }

  @Override
  public void onEnqueuedElements(int enqueuedElements, int newSize) {
    handle(() -> delegate.onEnqueuedElements(enqueuedElements, newSize));
  }

  @Override
  public void onWaitingForElements(@NonNull Deadline deadline) {
    handle(() -> delegate.onWaitingForElements(deadline));
  }

  @Override
  public void onDequeuedElements(int dequeuedElements, int newSize) {
    handle(() -> delegate.onDequeuedElements(dequeuedElements, newSize));
  }

  private void handle(Runnable callback) {
    try {
      callback.run();
    } catch (RuntimeException e) {
      log.error("Exception thrown from queue listener", e);
    }
  }
}
