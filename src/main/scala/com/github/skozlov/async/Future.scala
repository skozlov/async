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

    def map[B](f: A => B): Future[B] = flatMap {a => Future{f(a)}}

    def flatten[B](implicit ev: A <:< Future[B]): Future[B] = flatMap(ev)

    def zipWith[B, C](that: Future[B])(f: (A, B) => Future[C]): Future[C] = MultiStep(ForkJoin(this, that)(f))

    def zip[B](that: Future[B]): Future[(A, B)] = zipWith(that){(a, b) => Future{(a, b)}}
}

object Future {
    case class SingleStep[+A](f: () => A) extends Future[A]

    case class MultiStep[+A, F](start: ForkJoin[F, A]) extends Future[A]

    def apply[A](a: => A): Future[A] = SingleStep(() => a)
}
