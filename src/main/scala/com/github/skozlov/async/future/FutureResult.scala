package com.github.skozlov.async.future

sealed trait FutureResult[+A]

object FutureResult {
    case class Success[+A](value: A) extends FutureResult[A]

    case class Failure(e: Throwable) extends FutureResult[Nothing]

    case object Cancelled extends FutureResult[Nothing]
}
