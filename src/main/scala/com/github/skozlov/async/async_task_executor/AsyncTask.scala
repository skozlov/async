package com.github.skozlov.async.async_task_executor

import com.github.skozlov.async.cancel.Cancellable
import com.github.skozlov.async.deadline.Deadline

trait AsyncTask {
    this: AsyncTask with Cancellable.Mutable =>

    def checkStillNeeded(): Boolean = !checkCancelled() && !checkDeadlineOver()

    def perform(): Unit

    def deadline: Deadline

    def checkDeadlineOver(): Boolean = {
        if (deadline.isOver) {
            onDeadlineOver()
            true
        } else false
    }

    def onDeadlineOver(): Unit

    def checkCancelled(): Boolean = {
        if (cancelled) {
            onCancelConfirmation()
            true
        } else false
    }

    def onCancelConfirmation(): Unit
}
