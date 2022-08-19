package com.github.skozlov.async

trait AsyncTask{
    @volatile
    private var cancelling = false

    def checkStillNeeded(): Boolean = !checkCancelled() && !checkDeadlineOver()

    def perform(): Unit

    def deadline: Deadline

    def cancel(): Unit ={
        cancelling = true
    }

    def checkDeadlineOver(): Boolean = {
        if (deadline.isOver) {
            onDeadlineOver()
            true
        } else false
    }

    def onDeadlineOver(): Unit

    def checkCancelled(): Boolean = {
        if (cancelling) {
            onCancelled()
            true
        } else false
    }

    def onCancelled(): Unit
}
