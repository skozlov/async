package com.github.skozlov.async

import java.time.{Clock, Instant}
import java.time.temporal.ChronoUnit.NANOS
import scala.concurrent.duration.Duration

sealed trait Deadline extends Ordered[Deadline] {
    def toTimeout: Duration

    def isOver: Boolean = toTimeout <= Duration.Zero
}

object Deadline {
    case object Inf extends Deadline {
        override def compare(that: Deadline): Int = {
            if (this == that) 0 else 1
        }

        override val toTimeout: Duration = Duration.Inf
    }

    case object MinusInf extends Deadline {
        override def compare(that: Deadline): Int = {
            if (this == that) 0 else -1
        }

        override val toTimeout: Duration = Duration.MinusInf
    }

    case class At(instant: Instant, clock: Clock) extends Deadline {
        override def compare(that: Deadline): Int = that match {
            case Inf => -1
            case MinusInf => 1
            case At(thatInstant, _) => this.instant compareTo thatInstant
        }

        override def toTimeout: Duration = Duration.fromNanos(clock.instant().until(instant, NANOS))
    }

    implicit def fromTimeout(timeout: Duration)(implicit clock: Clock): Deadline = timeout match {
        case Duration.Inf => Inf
        case Duration.MinusInf => MinusInf
        case _ => At(clock.instant() plusNanos timeout.toNanos, clock)
    }
}
