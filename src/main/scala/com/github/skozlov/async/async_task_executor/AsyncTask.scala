package com.github.skozlov.async.async_task_executor

import com.github.skozlov.async.deadline.Deadline

trait AsyncTask {
    @volatile
    private var _cancelled = false

    def checkStillNeeded(): Boolean = !checkCancelled() && !checkDeadlineOver()

    def perform(): Unit

    def deadline: Deadline

    def cancel(): Unit = _cancelled = true

    def cancelled(): Boolean = _cancelled

    def checkDeadlineOver(): Boolean = {
        if (deadline.isOver) {
            onDeadlineOver()
            true
        } else false
    }

    def onDeadlineOver(): Unit

    def checkCancelled(): Boolean = {
        if (cancelled()) {
            onCancelConfirmation()
            true
        } else false
    }

    def onCancelConfirmation(): Unit
}
