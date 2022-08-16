package com.github.skozlov

import java.lang.Thread.{currentThread, interrupted}
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

	implicit class RichLock(wrapped: Lock) {
		def tryLock(timeout: Duration): Boolean ={
			require(timeout >= Duration.Zero, s"Negative timeout: $timeout")
			if (timeout.isFinite) {
				wrapped.tryLock(timeout.length, timeout.unit)
			} else {
				wrapped.lock()
				true
			}
		}

		@throws[TimeoutException]
		def locking[R](r: => R)(implicit lockTimeout: Duration): R = {
			if (!tryLock(lockTimeout)) {
				throw new TimeoutException(s"Could not acquire the lock for $lockTimeout")
			}
			try {
				r
			} finally {
				wrapped.unlock()
			}
		}
	}

	implicit class RichCondition(wrapped: Condition) {
		def await(timeout: Duration): Boolean = {
			require(timeout >= Duration.Zero, s"Negative timeout: $timeout")
			if (timeout.isFinite) {
				wrapped.await(timeout.length, timeout.unit)
			} else {
				wrapped.await()
				true
			}
		}
	}
}