package com.github.skozlov

import java.lang.Thread.interrupted

package object async {
	def workerThread(getNextTask: () => () => Any): Thread = new Thread(() =>
		try {
			while (!interrupted()) {
				val task = getNextTask()
				if (!Thread.currentThread().isInterrupted) {
					task()
				}
			}
		} catch {
			case _: InterruptedException =>
		}
	)
}