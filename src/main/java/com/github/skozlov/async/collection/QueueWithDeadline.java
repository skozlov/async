package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import lombok.NonNull;

import java.util.List;

import static java.util.Collections.singleton;

public interface QueueWithDeadline<E> {
  void enqueue(@NonNull Iterable<? extends E> elements, @NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException;

  default void enqueue(@NonNull E element, @NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException {
    enqueue(singleton(element), deadline);
  }

  @NonNull
  List<E> dequeue(int minElements, int maxElements, @NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException;

  default @NonNull E dequeueOne(@NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException {
    return dequeue(1, 1, deadline).getFirst();
  }
}
