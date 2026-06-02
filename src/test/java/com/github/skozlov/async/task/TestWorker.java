package com.github.skozlov.async.task;

import com.github.skozlov.async.collection.Queue;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TestWorker extends Worker {
  public final List<Event> events;

  public TestWorker(@NonNull Queue<Task> taskQueue) {
    super(taskQueue);
    events = new ArrayList<>();
  }

  @Override
  protected void onTaskError(@NonNull TaskException e) {
    events.add(new TaskError(e));
  }

  @Override
  protected boolean onWorkerError(@NonNull WorkerException e) {
    events.add(new WorkerError(e));
    return !(e.getCause() instanceof InterruptedException);
  }

  @Override
  protected void onWorkerCompletion() {
    events.add(WorkerCompletion.INSTANCE);
  }

  public sealed interface Event permits TaskError, WorkerCompletion, WorkerError {}

  public record TaskError(@NonNull TaskException e) implements Event {}

  public record WorkerError(@NonNull WorkerException e) implements Event {}

  public static final class WorkerCompletion implements Event {
    private WorkerCompletion() {}

    public static final WorkerCompletion INSTANCE = new WorkerCompletion();
  }
}
