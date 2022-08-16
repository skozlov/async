package com.github.skozlov.async

import scala.concurrent.duration.Duration
import scala.util.Try

trait Executor {
    def execute[R](r: => R)(implicit blockingTimeout: Duration): Option[Try[R]]
}

object Executor {
    def currentThread(): Executor = new Executor {
        override def execute[R](r: => R)(implicit blockingTimeout: Duration): Option[Try[R]] = Some(Try{r})
    }
}
