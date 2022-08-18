package com.github.skozlov

import java.lang.Math.max
import java.util.concurrent.locks.{Condition, Lock}
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

package object async {
	implicit class RichThread(thread: Thread){
		@throws[TimeoutException]
		def join(timeout: Duration): Unit = {
			if (thread.isAlive) {
				if (timeout <= Duration.Zero) {
					throw new TimeoutException(s"Will not join with non-positive timeout $timeout")
				} else if (timeout == Duration.Inf) {
					thread.join()
				} else {
					thread.join(max(1, timeout.toMillis)) // can't pass 0 millis here as it means forever
				}
			}
		}
	}

	implicit class RichLock(lock: Lock) {
		@throws[TimeoutException]
		def lockIn(timeout: Duration): Unit ={
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
		def locking[R](r: => R)(implicit deadline: Deadline): R ={
			lockIn(deadline.toTimeout)
			try {
				r
			} finally {
				lock.unlock()
			}
		}
	}

	implicit class RichCondition(condition: Condition) {
		@throws[TimeoutException]
		def await(timeout: Duration): Unit = {
			if (timeout <= Duration.Zero) {
				throw new TimeoutException(s"Will not await with non-positive timeout $timeout")
			} else if (timeout == Duration.Inf) {
				condition.await()
			} else {
				if (!condition.await(timeout.length, timeout.unit)) {
					throw new TimeoutException(s"Waited on condition for $timeout")
				}
			}
		}

		@throws[TimeoutException]
		def awaitWithDeadline()(implicit deadline: Deadline): Unit ={
			await(deadline.toTimeout)
		}
	}
}