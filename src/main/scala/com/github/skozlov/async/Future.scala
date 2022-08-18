package com.github.skozlov.async

import com.github.skozlov.async.Future.MultiStep
import Future._

sealed trait Future[+A]{
    def flatMap[B](f: A => Future[B]): Future[B] = {
        def afterThis(a: A): Future[B] = {
            MultiStep(ForkJoin(f(a)))
        }

        this match {
            case SingleStep(_) => MultiStep(ForkJoin(this, afterThis _))
            case MultiStep(startA) => MultiStep(
                startA.copy(
                    join = startA.join andThen {_ flatMap afterThis}
                )
            )
        }
    }
}

object Future {
    case class SingleStep[+A](f: () => A) extends Future[A]

    case class MultiStep[+A, F](start: ForkJoin[F, A]) extends Future[A]
}
