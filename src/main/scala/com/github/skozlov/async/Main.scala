package com.github.skozlov.async

import com.github.skozlov.async.async_task_executor.thread_pool.WorkerThread
import java.lang.Thread.sleep

// todo delete
object Main extends App {
	val thread = new WorkerThread(() => {
		println("Task start")
		sleep(1000)
		println("Task end")
	})
	thread.start()
	sleep(3500)
	println("Finishing")
	thread.finish()
}