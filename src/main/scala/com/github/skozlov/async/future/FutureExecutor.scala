package com.github.skozlov.async.future

import com.github.skozlov.async.deadline.Deadline

import scala.util.Try

trait FutureExecutor {
    def runAsync[A](future: Future[A])(implicit deadline: Deadline): Unit

    def await[A](future: Future[A])(implicit deadline: Deadline): Try[A]
}
