package com.github.skozlov.async.async_task_executor.thread_pool

import java.lang.Thread.currentThread

class WorkerThread(performNextTask: () => Any) extends Thread {
    @volatile
    private var _finishing = false

    def finish(): Unit = {
        _finishing = true
    }

    def finishing(): Boolean = {
        _finishing
    }

    override def run(): Unit = {
        while (!currentThread().isInterrupted && !_finishing) {
            performNextTask()
        }
    }
}
