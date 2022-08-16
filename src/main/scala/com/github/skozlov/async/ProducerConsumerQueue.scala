package com.github.skozlov.async

import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

class ProducerConsumerQueue[A] {
    private var in = new ArrayBuffer[A]()
    private var out = new ArrayBuffer[A]()
    private val inLock = new ReentrantLock()
    private val outLock = new ReentrantLock()
    private val condition = inLock.newCondition()

    @throws[TimeoutException]
    def enqueue(a: A)(implicit blockingTimeout: Duration): Unit ={
        inLock.locking {
            in append a
            condition.signalAll()
        }
    }

    @throws[TimeoutException]
    def dequeue()(implicit blockingTimeout: Duration): A = {
        outLock.locking {
            while (out.isEmpty) {
                inLock.locking {
                    while (in.isEmpty) {
                        condition.await(blockingTimeout)
                    }
                    out = in.reverse
                    in = new ArrayBuffer[A]()
                }
            }
            out remove (out.size - 1)
        }
    }
}
