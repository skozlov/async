package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.commons.Pair;
import lombok.NonNull;

import java.util.Iterator;
import java.util.List;

public interface Queue<E> {
  @NonNull
  Try<Pair<Integer, Iterator<? extends E>>> enqueue(@NonNull Iterable<? extends E> elements, @NonNull Deadline deadline);

  @NonNull
  Try<List<E>> dequeue(int minElements, int maxElements, @NonNull Deadline deadline);
}
