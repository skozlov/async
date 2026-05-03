package com.github.skozlov.async.function;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import lombok.NonNull;

public sealed interface PartialResult<R> {
  R asSuccess() throws UnsupportedOperationException;

  DeadlinePassed<R> asDeadlinePassed() throws UnsupportedOperationException;

  Interrupted<R> asInterrupted() throws UnsupportedOperationException;

  record Success<R>(R result) implements PartialResult<R> {
    @Override
    public R asSuccess() throws UnsupportedOperationException {
      return result;
    }

    @Override
    public DeadlinePassed<R> asDeadlinePassed() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Unexpected success: " + this);
    }

    @Override
    public Interrupted<R> asInterrupted() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Unexpected success: " + this);
    }
  }

  record DeadlinePassed<R>(@NonNull Deadline deadline, R partialResult, DeadlinePassedException exception) implements PartialResult<R> {
    @Override
    public DeadlinePassed<R> asDeadlinePassed() throws UnsupportedOperationException {
      return this;
    }

    @Override
    public R asSuccess() throws UnsupportedOperationException {
      throw new UnsupportedOperationException(deadline + " passed, partial result: " + partialResult, exception);
    }

    @Override
    public Interrupted<R> asInterrupted() throws UnsupportedOperationException {
      throw new UnsupportedOperationException(deadline + " passed, partial result: " + partialResult, exception);
    }
  }

  record Interrupted<R>(R partialResult, InterruptedException exception) implements PartialResult<R> {
    @Override
    public Interrupted<R> asInterrupted() throws UnsupportedOperationException {
      return this;
    }

    @Override
    public R asSuccess() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Interrupted, partial result: " + partialResult, exception);
    }

    @Override
    public DeadlinePassed<R> asDeadlinePassed() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Interrupted, partial result: " + partialResult, exception);
    }
  }
}
