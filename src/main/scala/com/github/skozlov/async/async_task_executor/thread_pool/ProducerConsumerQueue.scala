package com.github.skozlov.async.async_task_executor.thread_pool

import com.github.skozlov.async.{Deadline, RichCondition, RichLock}

import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.TimeoutException

class ProducerConsumerQueue[A] {
    private var in = new ArrayBuffer[A]()
    private var out = new ArrayBuffer[A]()
    private val inLock = new ReentrantLock()
    private val outLock = new ReentrantLock()
    private val condition = inLock.newCondition()

    @throws[TimeoutException]
    def enqueue(a: A)(implicit deadline: Deadline): Unit ={
        inLock.locking {
            in append a
            condition.signalAll()
        }
    }

    @throws[TimeoutException]
    def dequeue()(implicit deadline: Deadline): A = {
        outLock.locking {
            while (out.isEmpty) {
                inLock.locking {
                    while (in.isEmpty) {
                        condition.awaitWithDeadline()
                    }
                    out = in.reverse
                    in = new ArrayBuffer[A]()
                }
            }
            out remove (out.size - 1)
        }
    }
}
