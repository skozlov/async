package com.github.skozlov.async.lock.condition;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import com.github.skozlov.commons.CheckedSupplier;
import lombok.NonNull;

import java.util.concurrent.locks.Condition;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public interface SafeCondition {
  @NonNull
  Condition toUnsafe();

  default void await(@NonNull Deadline deadline, @NonNull CheckedSupplier<Boolean, InterruptedException> until) throws InterruptedException, DeadlinePassedException {
    var unsafe = toUnsafe();
    for (;;) {
      if (until.get()) {
        return;
      }
      var timeout = deadline.getTimeLeft();
      if (timeout.isPositive()) {
        //noinspection ResultOfMethodCallIgnored
        unsafe.await(timeout.toNanos(), NANOSECONDS);
      } else {
        throw new DeadlinePassedException(deadline);
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
