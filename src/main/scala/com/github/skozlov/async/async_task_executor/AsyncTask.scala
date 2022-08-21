package com.github.skozlov.async.async_task_executor

import com.github.skozlov.async.cancel.Cancellable
import com.github.skozlov.async.deadline.Deadline

import scala.concurrent.TimeoutException

trait AsyncTask {
    def checkStillNeeded(): Boolean = !checkCancelled() && !checkDeadlineOver()

    def perform(): Unit

    def deadline: Deadline

    def cancel: Cancellable

    def checkDeadlineOver(): Boolean = {
        if (deadline.isOver) {
            onDeadlineOver()
            true
        } else false
    }

    def onDeadlineOver(): Unit

    def onDeadlineOver(e: TimeoutException): Unit

    def checkCancelled(): Boolean = {
        if (cancel.cancelled) {
            onCancelConfirmation()
            true
        } else false
    }

    def onCancelConfirmation(): Unit
}
