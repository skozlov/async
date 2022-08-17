package com.github.skozlov.async

import scala.concurrent.TimeoutException
import scala.util.Try

trait Executor {
    @throws[TimeoutException]
    def execute[R](r: => R)(implicit deadline: Deadline): Try[R]
}
