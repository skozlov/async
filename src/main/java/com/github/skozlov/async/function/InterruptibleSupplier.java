package com.github.skozlov.async.function;

@FunctionalInterface
public interface InterruptibleSupplier<T> {
  T get() throws InterruptedException;
}
