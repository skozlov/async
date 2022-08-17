package com.github.skozlov

import java.lang.Thread.{currentThread, interrupted}
import java.time.Clock
import java.util.concurrent.locks.{Condition, Lock}
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

package object async {
	def workerThread(getNextTask: () => () => Any): Thread = new Thread(() =>
		while (!interrupted()) {
			val task: Option[() => Any] = {
				try {
					val nextTask = getNextTask()
					if (currentThread().isInterrupted) {
						None
					} else {
						Some(nextTask)
					}
				} catch {
					case _: InterruptedException =>
						currentThread().interrupt()
						None
				}
			}
			task foreach {_.apply()}
		}
	)

	implicit class RichLock(lock: Lock) {
		@throws[TimeoutException]
		def lockIn(timeout: Duration): Unit ={
			if (!lock.tryLock()) {
				if (timeout <= Duration.Zero) {
					throw new TimeoutException(s"Will not lock with negative timeout $timeout")
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
		def locking[R](r: => R)(implicit deadline: Deadline, clock: Clock): Unit ={
			lockIn(deadline.toTimeout)
			try {
				r
			} finally {
				lock.unlock()
			}
		}

		@throws[TimeoutException]
		@deprecated("", "")
		def lockingOld[R](r: => R)(implicit lockTimeout: Duration): R = { // todo delete
			lockIn(lockTimeout)
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
		def awaitWithDeadline(implicit deadline: Deadline, clock: Clock): Unit ={
			await(deadline.toTimeout)
		}
	}
}