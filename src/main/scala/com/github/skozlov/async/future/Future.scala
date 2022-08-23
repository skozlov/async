package com.github.skozlov.async.future

import com.github.skozlov.async.future.Future._

import scala.util.{Failure, Success, Try}

sealed trait Future[+A]{
    def flatMapTry[B](f: Try[A] => Future[B]): Future[B] = {
        def afterThis(a: Try[A]): Future[B] = {
            MultiStep(ForkJoin(f(a)))
        }

        this match {
            case Failed(e) => f(Failure(e))
            case SingleStep(_) => MultiStep(ForkJoin(this, afterThis _))
            case MultiStep(startA) => MultiStep(
                startA.copy(
                    join = startA.join andThen {
                        _ flatMapTry afterThis
                    }
                )
            )
        }
    }

    def transform[B](success: A => Future[B], failure: Throwable => Future[B]): Future[B] = flatMapTry {
        case Success(a) => success(a)
        case Failure(e) => failure(e)
    }

    def flatMap[B](f: A => Future[B]): Future[B] = transform(f, Future.failed)

    def map[B](f: A => B): Future[B] = flatMap {a => Future{f(a)}}

    def flatten[B](implicit ev: A <:< Future[B]): Future[B] = flatMap(ev)

    def recoverWith[B >: A](f: PartialFunction[Throwable, Future[B]]): Future[B] = this match {
        case Failed(e) if f isDefinedAt e => f(e)
        case _ => this
    }

    def recover[B >: A](f: PartialFunction[Throwable, B]): Future[B] = ???
}

object Future {
    case class Failed(e: Throwable) extends Future[Nothing]

    case class SingleStep[+A](f: () => A) extends Future[A]

    case class MultiStep[+A, F](start: ForkJoin[F, A]) extends Future[A]

    def apply[A](a: => A): Future[A] = SingleStep(() => a)

    def failed(e: Throwable): Future[Nothing] = Failed(e)

    def joinWith[F, J](futures: Seq[Future[F]])(f: Seq[Try[F]] => Future[J]): Future[J] = MultiStep(ForkJoin(futures, f))

    def join[A](futures: Seq[Future[A]]): Future[Seq[Try[A]]] = joinWith(futures){Future(_)}
}
