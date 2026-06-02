package com.github.skozlov.commons;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Exception> {
  void accept(T t) throws E;
}
