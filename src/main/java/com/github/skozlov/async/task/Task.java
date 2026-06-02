package com.github.skozlov.async.task;

import com.github.skozlov.async.cancel.CancelException;
import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class Task {
  @NonNull
  public final Deadline deadline;

  private volatile Thread thread;
  private volatile boolean cancelled = false;
  private volatile boolean finished = false;

  public void cancel(boolean interrupt) {
    if (finished) {
      return;
    }
    cancelled = true;
    if (interrupt && thread != null) {
      thread.interrupt();
    }
  }

  public void execute() throws Exception {
    try {
      if (deadline.isPassed()) {
        throw new DeadlinePassedException(deadline);
      } else if (cancelled) {
        throw new CancelException();
      } else {
        thread = Thread.currentThread();
        if (thread.isInterrupted()) {
          throw new InterruptedException();
        } else {
          try {
            executeImpl();
          } catch (InterruptedException e) {
            if (cancelled) {
              throw new CancelException(e);
            } else {
              throw e;
            }
          }
        }
      }
    } finally {
      finished = true;
    }
  }

  protected abstract void executeImpl() throws Exception;
}
