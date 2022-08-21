package com.github.skozlov.async.async_task_executor.thread_pool

import com.github.skozlov.async.async_task_executor.{AsyncTask, AsyncTaskExecutor}
import com.github.skozlov.async.deadline.Deadline
import com.github.skozlov.async.thread.RichThread

import java.time.Clock
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

trait ThreadPool extends AsyncTaskExecutor {
    protected val tasks = new ProducerConsumerQueue[AsyncTask]

    @volatile
    private var finishDeadline: Option[Deadline] = None

    override def submit(task: AsyncTask): Unit = {
        if (task.checkStillNeeded()) {
            try {
                tasks.enqueue(task)(task.deadline)
            } catch {
                case e: TimeoutException => task.onDeadlineOver(e)
            }
        }
    }

    def start(): Unit

    @throws[TimeoutException]
    def finish(deadline: Deadline): Unit = {
        finishDeadline = Some(deadline)
        finishThreads(deadline)
    }

    @throws[TimeoutException]
    protected def finishThreads(deadline: Deadline): Unit

    def finishingWithDeadline(): Option[Deadline] = finishDeadline

    protected def newThread()(implicit taskWaitTimeout: Duration, clock: Clock): WorkerThread = {
        new WorkerThread(() => {
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
            private val threads = Array.tabulate(numberOfThreads){_ => newThread()}

            override def start(): Unit = threads foreach {_.start()}

            @throws[TimeoutException]
            override protected def finishThreads(deadline: Deadline): Unit = {
                threads foreach {_.finish()}
                threads foreach {_.join(deadline.toTimeout)}
            }
        }
    }
}
