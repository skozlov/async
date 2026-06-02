package com.github.skozlov.async.deadline;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static java.lang.Math.max;
import static java.time.temporal.ChronoUnit.NANOS;

@RequiredArgsConstructor
public class Deadline implements Comparable<Deadline> {
  @NonNull
  private final Instant instant;

  @NonNull
  private final Clock clock;

  public @NonNull Instant toInstant() {
    return instant;
  }

  public boolean isPassed() {
    return !clock.instant().isBefore(instant);
  }

  public @NonNull Duration getTimeLeft() {
    return Duration.ofNanos(max(0L, clock.instant().until(instant, NANOS)));
  }

  @Override
  public int compareTo(@NonNull Deadline that) {
    return this.instant.compareTo(that.instant);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Deadline deadline = (Deadline) o;
    return Objects.equals(instant, deadline.instant) && Objects.equals(clock, deadline.clock);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instant, clock);
  }

  @Override
  public String toString() {
    return "Deadline(" + instant + ")";
  }
}
