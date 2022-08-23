package com.github.skozlov.async.future

import com.github.skozlov.async.future.Future._

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

sealed trait Future[+A] {
    def flatMapTry[B](f: Try[A] => Future[B]): Future[B] = ForkJoin[A, B, Unit](
        fork = Seq(this),
        createJoinBuffer = () => (),
        join = (_, _, result) => Right(f(result))
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

    def zip[B](that: Future[B]): Future[(A, B)] = join(Seq(this, that)) map {results =>
        //noinspection ZeroIndexToHead
        (results(0).asInstanceOf[A], results(1).asInstanceOf[B])}
}

object Future {
    case class Completed[+A](result: Try[A]) extends Future[A]

    case class SingleStep[+A](f: () => A) extends Future[A]

    type FutureIndex = Int

    case class ForkJoin[F, +J, Buf](
        fork: Seq[Future[F]],
        createJoinBuffer: () => Buf,
        join: (Buf, FutureIndex, Try[F]) => Either[Buf, Future[J]]
    ) extends Future[J]

    def success[A](result: A): Future[A] = Completed(Success(result))

    def failure(e: Throwable): Future[Nothing] = Completed(Failure(e))

    def apply[A](result: => A): Future[A] = SingleStep{() => result}

    def join[A: ClassTag](futures: Seq[Future[A]]): Future[Seq[A]] = {
        val finalFuturesCompletedBefore = futures.size - 1
        ForkJoin[A, Seq[A], (Array[A], Int)](
            fork = futures,
            createJoinBuffer = () => (Array.ofDim[A](futures.size), 0),
            join = {
                case ((results, futuresCompletedBefore), i, Success(a)) =>
                    results(i) = a
                    if (futuresCompletedBefore == finalFuturesCompletedBefore) {
                        Right(Future.success(results.toSeq))
                    } else {
                        Left((results, futuresCompletedBefore + 1))
                    }
                case (_, _, Failure(e)) => Right(Future.failure(e))
            }
        )
    }

    def first[A](futures: Seq[Future[A]])(p: A => Boolean): Future[Option[A]] = ???
}
