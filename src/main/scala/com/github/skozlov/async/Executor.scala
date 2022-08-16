package com.github.skozlov.async

import scala.concurrent.duration.Duration
import scala.util.Try

trait Executor {
    def execute[R](f: () => R)(implicit timeout: Duration): Option[Try[R]]
}
