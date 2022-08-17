package com.github.skozlov.async

case class ForkJoin[F, +J](fork: Seq[Future[F]], join: Seq[F] => Future[J])

object ForkJoin{
    def apply[A, B](a: Future[A], aToB: A => Future[B]): ForkJoin[A, B] = {
        ForkJoin(Seq(a), (results: Seq[A]) => aToB(results.head))
    }
}
