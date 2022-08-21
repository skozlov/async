package com.github.skozlov.async.future.executor

import com.github.skozlov.async.cancel.Cancellable
import com.github.skozlov.async.deadline.Deadline
import com.github.skozlov.async.future.Future

trait FutureExecutor {
    def runAsync[A](future: Future[A])(implicit deadline: Deadline, cancel: Cancellable): Unit

    def get[A](future: Future[A])(implicit deadline: Deadline, cancel: Cancellable): FutureResult[A]
}
