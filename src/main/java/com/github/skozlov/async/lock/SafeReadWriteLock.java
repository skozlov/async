package com.github.skozlov.async.lock;

import lombok.NonNull;

import java.util.concurrent.locks.ReadWriteLock;

public interface SafeReadWriteLock {
  @NonNull ReadWriteLock toUnsafe();

  default @NonNull SafeLock readLock() {
    return SafeLock.from(toUnsafe().readLock());
  }

  default @NonNull SafeLock writeLock() {
    return SafeLock.from(toUnsafe().writeLock());
  }

  static @NonNull SafeReadWriteLock from(@NonNull ReadWriteLock unsafe) {
    return new SimpleSafeReadWriteLock(unsafe);
  }
}
