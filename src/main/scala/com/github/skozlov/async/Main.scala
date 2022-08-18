package com.github.skozlov.async

import java.lang.Thread.sleep

object Main extends App {
	val thread = workerThread(() => {
		println("Executing a task...")
		sleep(1000)
	})
	thread.start()
	sleep(3500)
	thread.interrupt()
}