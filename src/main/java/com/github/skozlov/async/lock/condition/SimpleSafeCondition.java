package com.github.skozlov.async.lock.condition;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.locks.Condition;

@RequiredArgsConstructor
public class SimpleSafeCondition implements SafeCondition {
  private final @NonNull Condition unsafe;

  @Override
  public @NonNull Condition toUnsafe() {
    return unsafe;
  }
}
