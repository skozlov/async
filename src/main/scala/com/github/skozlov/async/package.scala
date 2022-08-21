package com.github.skozlov

import java.lang.Math.max
import java.util.concurrent.locks.Condition
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