package com.github.skozlov.async.future.executor

import com.github.skozlov.async.cancel.Cancellable
import com.github.skozlov.async.deadline.Deadline
import com.github.skozlov.async.future.Future

trait FutureExecutor {
    def foreachAsync[A]
        (future: Future[A])
        (f: FutureResult[A] => Unit)
        (implicit deadline: Deadline, cancel: Cancellable)
        : Unit

    def get[A](future: Future[A])(implicit deadline: Deadline, cancel: Cancellable): FutureResult[A]
}
