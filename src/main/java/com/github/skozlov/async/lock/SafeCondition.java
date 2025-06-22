package com.github.skozlov.async.lock;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.function.InterruptibleSupplier;
import lombok.NonNull;

import java.util.concurrent.locks.Condition;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public interface SafeCondition {
  @NonNull
  Condition toUnsafe();

  default boolean await(@NonNull Deadline deadline, @NonNull InterruptibleSupplier<Boolean> until) throws InterruptedException {
    var unsafe = toUnsafe();
    for (;;) {
      if (until.get()) {
        return true;
      }
      var timeout = deadline.getTimeLeft();
      if (timeout.isPositive()) {
        //noinspection ResultOfMethodCallIgnored
        unsafe.await(timeout.toNanos(), NANOSECONDS);
      } else {
        return false;
      }
    }
  }

  default void signal() {
    toUnsafe().signal();
  }

  default void signalAll() {
    toUnsafe().signalAll();
  }

  static @NonNull SafeCondition from(@NonNull Condition unsafe) {
    return new SimpleSafeCondition(unsafe);
  }
}
