package com.github.skozlov.async

import java.time.Clock
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

trait ThreadPool extends AsyncTaskExecutor {
    protected val tasks = new ProducerConsumerQueue[AsyncTask]

    override def submit(task: AsyncTask): Unit = {
        if (task.checkStillNeeded()) {
            try {
                tasks.enqueue(task)(task.deadline)
            } catch {
                case _: TimeoutException => task.onDeadlineOver()
            }
        }
    }

    protected def newThread()(implicit taskWaitTimeout: Duration, clock: Clock): Thread = {
        workerThread(() => {
            val task = tasks.dequeue()(Deadline.fromTimeout(taskWaitTimeout))
            if (task.checkStillNeeded()) {
                task.perform()
            }
        })
    }
}

object ThreadPool {
    def fixed(numberOfThreads: Int)(implicit taskWaitTimeout: Duration, clock: Clock): ThreadPool = {
        require(numberOfThreads > 0, s"Required at least 1 thread but found $numberOfThreads")
        new ThreadPool {
            for (_ <- 1 to numberOfThreads) {
                newThread().start()
            }
        }
    }
}
