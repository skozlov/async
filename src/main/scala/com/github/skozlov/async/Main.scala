package com.github.skozlov.async

import java.lang.Thread.sleep

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