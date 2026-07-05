package com.github.skozlov.async.task;

import com.github.skozlov.async.collection.queue.Queue;
import com.github.skozlov.async.collection.Try;
import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class Worker {
  @NonNull
  private final Queue<Task> taskQueue;

  private volatile Thread thread;
  private volatile boolean shutDownRequested = false;
  private volatile boolean completed = false;

  public void execute(@NonNull Deadline deadline) {
    try {
      thread = Thread.currentThread();
      while (!shutDownRequested) {
        try {
          Try<List<Task>> task = taskQueue.dequeue(1, 1, deadline);
          switch (task) {
            case Try.Success<List<Task>> s -> executeTask(s.result().getFirst());
            case Try.Failure<List<Task>> f -> {
              var e = f.exception();
              if (!(shutDownRequested && e instanceof InterruptedException)) {
                throw e;
              }
            }
          }
        } catch (Exception e) {
          if (onWorkerError(new WorkerException(e)) || e instanceof DeadlinePassedException) {
            break;
          }
        }
      }
    } finally {
      onWorkerCompletion();
      completed = true;
    }
  }

  public void shutDown(boolean interrupt) {
    if (completed) {
      return;
    }
    shutDownRequested = true;
    if (interrupt && thread != null) {
      thread.interrupt();
    }
  }

  protected void executeTask(@NonNull Task task) {
    try {
      task.execute();
    } catch (Exception e) {
      onTaskError(new TaskException(e));
    }
  }

  protected abstract void onTaskError(@NonNull TaskException e);

  protected abstract boolean onWorkerError(@NonNull WorkerException e);

  protected abstract void onWorkerCompletion();
}
