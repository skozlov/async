package com.github.skozlov.commons;

@FunctionalInterface
public interface CheckedRunnable<E extends Exception> {
  void run() throws E;
}
