package com.github.skozlov.async.collection.queue;

import com.github.skozlov.async.deadline.Deadline;
import lombok.NonNull;

public interface QueueListener {
  void onWaitingForFreeSpace(@NonNull Deadline deadline);

  void onEnqueuedElements(int enqueuedElements, int newSize);

  void onWaitingForElements(@NonNull Deadline deadline);

  void onDequeuedElements(int dequeuedElements, int newSize);
}
