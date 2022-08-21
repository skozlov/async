package com.github.skozlov.async

import java.util.concurrent.locks.Lock
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

package object lock {
    implicit class RichLock(lock: Lock) {
        @throws[TimeoutException]
        def lockIn(timeout: Duration): Unit = {
            if (!lock.tryLock()) {
                if (timeout <= Duration.Zero) {
                    throw new TimeoutException(s"Will not lock with non-positive timeout $timeout")
                } else if (timeout == Duration.Inf) {
                    lock.lock()
                } else {
                    if (!lock.tryLock(timeout.length, timeout.unit)) {
                        throw new TimeoutException(s"Could not acquire the lock in $timeout")
                    }
                }
            }
        }

        @throws[TimeoutException]
        def locking[R](r: => R)(implicit deadline: Deadline): R = {
            lockIn(deadline.toTimeout)
            try {
                r
            } finally {
                lock.unlock()
            }
        }
    }
}
