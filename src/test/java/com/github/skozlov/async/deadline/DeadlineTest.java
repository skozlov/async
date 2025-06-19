package com.github.skozlov.async.deadline;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class DeadlineTest {
  @Test
  void toInstantTest() {
    var instant = Instant.ofEpochMilli(123);
    var clock = Clock.fixed(Instant.EPOCH, UTC);
    assertThat(new Deadline(instant, clock).toInstant()).isEqualTo(instant);
  }

  @Test
  void getTimeLeftTest() {
    var now = Instant.ofEpochMilli(1000);
    var clock = Clock.fixed(now, UTC);
    assertThat(new Deadline(now.plusMillis(1), clock).getTimeLeft()).isEqualTo(Duration.ofMillis(1));
    assertThat(new Deadline(now, clock).getTimeLeft()).isEqualTo(Duration.ZERO);
    assertThat(new Deadline(now.minusSeconds(1), clock).getTimeLeft()).isEqualTo(Duration.ZERO);
  }

  @Test
  void compareToTest() {
    var clock = Clock.fixed(Instant.EPOCH, UTC);
    var min = new Deadline(Instant.ofEpochMilli(123), clock);
    var max = new Deadline(Instant.ofEpochMilli(456), clock);
    assertThat(min.compareTo(max)).isNegative();
    assertThat(max.compareTo(min)).isPositive();
    //noinspection EqualsWithItself
    assertThat(min.compareTo(min)).isZero();
  }

  @Test
  void equalsTest() {
    var instant1 = Instant.ofEpochMilli(1);
    var instant2 = Instant.ofEpochMilli(2);
    var clock1 = Clock.fixed(Instant.EPOCH, UTC);
    var clock2 = Clock.fixed(Instant.ofEpochMilli(1000), UTC);
    assertThat(new Deadline(instant1, clock1)).isEqualTo(new Deadline(instant1, clock1));
    assertThat(new Deadline(instant1, clock1)).isNotEqualTo(new Deadline(instant2, clock1));
    assertThat(new Deadline(instant1, clock1)).isNotEqualTo(new Deadline(instant1, clock2));
    assertThat(new Deadline(instant1, clock1)).isNotEqualTo(new Deadline(instant2, clock2));
  }

  @Test
  void hashCodeTest() {
    var instant = Instant.ofEpochMilli(1);
    var clock = Clock.fixed(Instant.EPOCH, UTC);
    assertThat(new Deadline(instant, clock).hashCode()).isEqualTo(new Deadline(instant, clock).hashCode());
  }

  @Test
  void toStringTest() {
    assertThat(new Deadline(Instant.ofEpochMilli(1), Clock.fixed(Instant.EPOCH, UTC)).toString())
        .isEqualTo("Deadline(1970-01-01T00:00:00.001Z)");
  }
}
