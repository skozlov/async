package com.github.skozlov.async.lock;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import lombok.NonNull;

import java.time.Duration;
import java.util.concurrent.locks.Lock;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public interface SafeLock {
  @NonNull
  Lock toUnsafe();

  default @NonNull LockAutoClose lock(@NonNull Deadline deadline) throws InterruptedException, DeadlinePassedException {
    Lock unsafe = toUnsafe();
    for (;;) {
      Duration timeout = deadline.getTimeLeft();
      boolean acquired = unsafe.tryLock(timeout.toNanos(), NANOSECONDS);
      if (acquired) {
        return new LockAutoClose(unsafe);
      } else if (!timeout.isPositive()) {
        throw new DeadlinePassedException(deadline);
      }
    }
  }

  default @NonNull SafeCondition newCondition() {
    return SafeCondition.from(toUnsafe().newCondition());
  }

  static @NonNull SafeLock from(@NonNull Lock unsafe) {
    return new SimpleSafeLock(unsafe);
  }
}
