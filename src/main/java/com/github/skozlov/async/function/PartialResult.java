package com.github.skozlov.async.function;

import com.github.skozlov.async.deadline.DeadlinePassedException;

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

  record DeadlinePassed<R>(R partialResult, DeadlinePassedException exception) implements PartialResult<R> {
    @Override
    public DeadlinePassed<R> asDeadlinePassed() throws UnsupportedOperationException {
      return this;
    }

    @Override
    public R asSuccess() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Deadline passed, partial result: " + partialResult, exception);
    }

    @Override
    public Interrupted<R> asInterrupted() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Deadline passed, partial result: " + partialResult, exception);
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
