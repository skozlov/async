package com.github.skozlov.async

import java.lang.Thread.sleep

object Main extends App {
	val thread = workerThread(() => {
		println("Waiting for a next task...")
		sleep(1000)
		() => {
			println("Executing a task...")
			sleep(1000)
		}
	})
	thread.start()
	sleep(3000)
	thread.interrupt()
}