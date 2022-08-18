package com.github.skozlov.async

trait AsyncTaskExecutor{
    def submit(task: AsyncTask): Unit
}

object AsyncTaskExecutor{
    val Immediate: AsyncTaskExecutor = (task: AsyncTask) => if (task.checkStillNeeded()) task.perform()
}
