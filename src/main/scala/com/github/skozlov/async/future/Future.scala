package com.github.skozlov.async.future

import com.github.skozlov.async.future.Future._

import scala.reflect.ClassTag
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

    def completed[A](result: Try[A]): Future[A] = Completed(result)

    def success[A](result: A): Future[A] = completed(Success(result))

    def failure(e: Throwable): Future[Nothing] = completed(Failure(e))

    def apply[A](result: => A): Future[A] = SingleStep{() => result}

    def first[A](futures: Seq[Future[A]], failOnError: Boolean = false)(p: A => Boolean): Future[Option[A]] = {
        ForkJoin[A, Option[A], Try[Option[A]]](
            fork = futures,
            createJoinBuffer = () => Success(None),
            joinNext = (buf, _, result: Try[A]) => result match {
                case Success(a) if p(a) => (Success(Some(a)), true)
                case Failure(e) if failOnError => (Failure[Option[A]](e), true)
                case _ => (buf, false)
            },
            getResult = Future.completed
        )
    }

    def seq[A: ClassTag](futures: Seq[Future[A]]): Future[Seq[A]] = ForkJoin[A, Seq[A], Try[Array[A]]](
        fork = futures,
        createJoinBuffer = () => Success(Array.ofDim[A](futures.size)),
        joinNext = {
            case (_, _, Failure(e)) => (Failure(e), true)
            case (array, i, Success(a)) =>
                array.get(i) = a
                (array, false)
        },
        getResult = buffer => Future.completed(buffer map {_.toSeq})
    )

    def seqCollectingAllFailures[A](futures: Seq[Future[A]]): Future[Seq[Try[A]]] = ???
}
