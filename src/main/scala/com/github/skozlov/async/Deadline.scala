package com.github.skozlov.async

import java.time.{Clock, Instant}
import java.time.temporal.ChronoUnit.NANOS
import scala.concurrent.duration.Duration

sealed trait Deadline extends Ordered[Deadline] {
    def toTimeout(from: Instant): Duration

    implicit def toTimeout(implicit clock: Clock): Duration = toTimeout(from = clock.instant())
}

object Deadline {
    case object Inf extends Deadline {
        override def compare(that: Deadline): Int = {
            if (this == that) 0 else 1
        }

        override def toTimeout(from: Instant): Duration = Duration.Inf
    }

    case object MinusInf extends Deadline {
        override def compare(that: Deadline): Int = {
            if (this == that) 0 else -1
        }

        override def toTimeout(from: Instant): Duration = Duration.MinusInf
    }

    case class At(instant: Instant) extends Deadline {
        override def compare(that: Deadline): Int = that match {
            case Inf => -1
            case MinusInf => 1
            case At(thatInstant) => this.instant compareTo thatInstant
        }

        override def toTimeout(from: Instant): Duration = Duration.fromNanos(from.until(instant, NANOS))
    }

    def apply(from: Instant, timeout: Duration): Deadline = timeout match {
        case Duration.Inf => Inf
        case Duration.MinusInf => MinusInf
        case _ => At(from plusNanos timeout.toNanos)
    }

    implicit def fromTimeout(timeout: Duration)(implicit clock: Clock): Deadline = {
        Deadline(from = clock.instant(), timeout)
    }
}
