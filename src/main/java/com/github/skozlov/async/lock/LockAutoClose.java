package com.github.skozlov.async.lock;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.locks.Lock;

@RequiredArgsConstructor
public class LockAutoClose implements AutoCloseable {
  private final @NonNull Lock lock;

  @Override
  public void close() {
    lock.unlock();
  }
}
