package com.github.skozlov.async

trait AsyncTask{
    def perform(): Unit

    def deadline: Deadline

    def onDeadlineOver(): Unit

    def checkStillNeeded(): Boolean
}
