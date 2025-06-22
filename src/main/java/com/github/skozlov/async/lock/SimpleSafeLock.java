package com.github.skozlov.async.lock;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.locks.Lock;

@RequiredArgsConstructor
public class SimpleSafeLock implements SafeLock {
  private final @NonNull Lock unsafe;

  @Override
  public @NonNull Lock toUnsafe() {
    return unsafe;
  }
}
