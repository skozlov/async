package com.github.skozlov.async

case class ForkJoin[F, +J](fork: Seq[Future[F]], join: Seq[F] => Future[J])

object ForkJoin{
    def apply[F, J](future: => Future[J]): ForkJoin[F, J] = ForkJoin(Seq.empty, (_: Seq[F]) => future)

    def apply[A, B](a: Future[A], aToB: A => Future[B]): ForkJoin[A, B] = {
        ForkJoin(Seq(a), (results: Seq[A]) => aToB(results.head))
    }

    def apply[F, A <: F, B <: F, C](a: Future[A], b: Future[B])(f: (A, B) => Future[C]): ForkJoin[F, C] = {
        //noinspection ZeroIndexToHead
        ForkJoin(Seq(a, b), (results: Seq[F]) => f(results(0).asInstanceOf[A], results(1).asInstanceOf[B]))
    }
}
