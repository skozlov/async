package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.function.PartialResult;
import com.github.skozlov.commons.Pair;
import lombok.NonNull;

import java.util.Iterator;
import java.util.List;

public interface QueueWithDeadline<E> {
  @NonNull
  PartialResult<Pair<Integer, Iterator<? extends E>>> enqueue(@NonNull Iterable<? extends E> elements, @NonNull Deadline deadline);

  @NonNull
  PartialResult<List<E>> dequeue(int minElements, int maxElements, @NonNull Deadline deadline);
}
