package com.github.skozlov.async.lock;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.locks.ReadWriteLock;

@RequiredArgsConstructor
public class SimpleSafeReadWriteLock implements SafeReadWriteLock {
  private final @NonNull ReadWriteLock unsafe;

  @Override
  public @NonNull ReadWriteLock toUnsafe() {
    return unsafe;
  }
}
