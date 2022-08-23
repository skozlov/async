package com.github.skozlov.async.future

import com.github.skozlov.async.future.Future._

import scala.util.{Failure, Success, Try}

sealed trait Future[+A] {
    def flatMapTry[B](f: Try[A] => Future[B]): Future[B] = ForkJoin[A, B, Option[Future[B]]](
        fork = Seq(this),
        createJoinBuffer = () => None,
        joinNext = (_, _, result) => (Some(f(result)), true),
        getResult = _.get
    )

    def flatMap[B](f: A => Future[B]): Future[B] = flatMapTry {
        case Success(a) => f(a)
        case Failure(e) => Future.failure(e)
    }

    def map[B](f: A => B): Future[B] = flatMap {a => Future.success(f(a))}

    def recover[B >: A](f: PartialFunction[Throwable, Future[B]]): Future[B] = flatMapTry {
        case Failure(e) if f isDefinedAt e => f(e)
        case _ => this
    }
}

object Future {
    case class Completed[+A](result: Try[A]) extends Future[A]

    case class SingleStep[+A](f: () => A) extends Future[A]

    type FutureIndex = Int

    type JoinComplete = Boolean

    case class ForkJoin[F, +J, Buf](
        fork: Seq[Future[F]],
        createJoinBuffer: () => Buf,
        joinNext: (Buf, FutureIndex, Try[F]) => (Buf, JoinComplete),
        getResult: Buf => Future[J]
    ) extends Future[J]

    def success[A](result: A): Future[A] = Completed(Success(result))

    def failure(e: Throwable): Future[Nothing] = Completed(Failure(e))

    def apply[A](result: => A): Future[A] = SingleStep{() => result}

    def first[A](futures: Seq[Future[A]])(p: A => Boolean): Future[Option[A]] = ???
}
