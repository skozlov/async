package com.github.skozlov.commons;

@FunctionalInterface
public interface CheckedSupplier<R, E extends Exception> {
  R get() throws E;
}
