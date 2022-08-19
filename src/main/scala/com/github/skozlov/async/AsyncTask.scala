package com.github.skozlov.async

trait AsyncTask{
    def checkStillNeeded(): Boolean = !checkDeadlineOver()

    def perform(): Unit

    def deadline: Deadline

    def onDeadlineOver(): Unit

    def checkDeadlineOver(): Boolean = {
        if (deadline.isOver) {
            onDeadlineOver()
            true
        } else false
    }
}
