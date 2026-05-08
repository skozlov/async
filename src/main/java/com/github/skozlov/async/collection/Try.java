package com.github.skozlov.async.collection;

import lombok.NonNull;

public sealed interface Try<R> {
  R asSuccessOrThrow() throws UnsupportedOperationException;

  Failure<R> asFailureOrThrow() throws UnsupportedOperationException;

  record Success<R>(R result) implements Try<R> {
    @Override
    public R asSuccessOrThrow() {
      return result;
    }

    @Override
    public Failure<R> asFailureOrThrow() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Not a Failure");
    }
  }

  record Failure<R>(@NonNull Exception exception, R partialResult) implements Try<R> {
    @Override
    public R asSuccessOrThrow() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Failure", exception);
    }

    @Override
    public Failure<R> asFailureOrThrow() {
      return this;
    }
  }

  static <R> Success<R> success(R result) {
    return new Success<>(result);
  }

  static <R> Failure<R> failure(@NonNull Exception exception, R partialResult) {
    return new Failure<>(exception, partialResult);
  }
}
