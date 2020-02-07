package com.github.skozlov

import java.lang.Thread.{currentThread, interrupted}

package object async {
	def workerThread(getNextTask: () => () => Any): Thread = new Thread(() =>
		while (!interrupted()) {
			val task: Option[() => Any] = {
				try {
					Some(getNextTask())
				} catch {
					case _: InterruptedException =>
						currentThread().interrupt()
						None
				}
			}
			task foreach {_.apply()}
		}
	)
}