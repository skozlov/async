package com.github.skozlov

import java.lang.Math.max
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
}