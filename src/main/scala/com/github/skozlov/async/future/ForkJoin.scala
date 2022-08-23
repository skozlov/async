package com.github.skozlov.async.future

import scala.util.Try

case class ForkJoin[F, +J](fork: Seq[Future[F]], join: Seq[Try[F]] => Future[J])

object ForkJoin{
    def apply[F, J](future: => Future[J]): ForkJoin[F, J] = ForkJoin(Seq.empty, (_: Seq[Try[F]]) => future)

    def apply[A, B](a: Future[A], aToB: Try[A] => Future[B]): ForkJoin[A, B] = {
        ForkJoin(Seq(a), (results: Seq[Try[A]]) => aToB(results.head))
    }
}
