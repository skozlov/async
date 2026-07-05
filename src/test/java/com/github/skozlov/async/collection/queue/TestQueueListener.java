package com.github.skozlov.async.collection.queue;

import com.github.skozlov.async.deadline.Deadline;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TestQueueListener implements QueueListener {
  public final List<Event> events = new ArrayList<>();

  @Override
  public void onWaitingForFreeSpace(@NonNull Deadline deadline) {
    events.add(new OnWaitingForFreeSpace(deadline));
  }

  @Override
  public void onEnqueuedElements(int enqueuedElements, int newSize) {
    events.add(new OnEnqueuedElements(enqueuedElements, newSize));
  }

  @Override
  public void onWaitingForElements(@NonNull Deadline deadline) {
    events.add(new OnWaitingForElements(deadline));
  }

  @Override
  public void onDequeuedElements(int dequeuedElements, int newSize) {
    events.add(new OnDequeuedElements(dequeuedElements, newSize));
  }

  public sealed interface Event {}

  public record OnWaitingForFreeSpace(@NonNull Deadline deadline) implements Event {}

  public record OnEnqueuedElements(int enqueuedElements, int newSize) implements Event {}

  public record OnWaitingForElements(@NonNull Deadline deadline) implements Event {}

  public record OnDequeuedElements(int dequeuedElements, int newSize) implements Event {}
}
