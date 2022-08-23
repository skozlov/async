package com.github.skozlov.async.future

import com.github.skozlov.async.future.Future.{ForkJoin, join}

import scala.Function.unlift
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

sealed trait Future[+A] {
    def flatMapTry[B](f: Try[A] => Future[B]): Future[B] = ForkJoin[A, B](
        Seq(this),
        {case Seq(Some(result)) => f(result)}
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

    case class ForkJoin[F, +J](
        fork: Seq[Future[F]],
        join: PartialFunction[Seq[Option[Try[F]]], Future[J]]
    ) extends Future[J]

    def success[A](result: A): Future[A] = Completed(Success(result))

    def failure(e: Throwable): Future[Nothing] = Completed(Failure(e))

    def apply[A](result: => A): Future[A] = SingleStep{() => result}

    def join[A](futures: Seq[Future[A]]): Future[Seq[A]] = ForkJoin[A, Seq[A]](
        futures,
        unlift {results: Seq[Option[Try[A]]] =>
            val joined = new ListBuffer[A]
            val iterator = results.iterator

            @tailrec
            def loop(): Option[Future[Seq[A]]] = {
                if (iterator.hasNext) {
                    iterator.next() match {
                        case None => None
                        case Some(Failure(e)) => Some(Future.failure(e))
                        case Some(Success(a)) =>
                            joined append a
                            loop()
                    }
                } else {
                    Some(Future.success(joined.toSeq))
                }
            }

            loop()
        }
    )

    def first[A](futures: Seq[Future[A]])(p: A => Boolean): Future[Option[A]] = ???
}
