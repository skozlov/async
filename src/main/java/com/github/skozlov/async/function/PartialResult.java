package com.github.skozlov.async.function;

import com.github.skozlov.async.deadline.DeadlinePassedException;

public sealed interface PartialResult<R> {
  record Success<R>(R result) implements PartialResult<R> {}

  record DeadlinePassed<R>(R partialResult, DeadlinePassedException exception) implements PartialResult<R> {}

  record Interrupted<R>(R partialResult, InterruptedException exception) implements PartialResult<R> {}
}
