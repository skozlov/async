package com.github.skozlov.async.async_task_executor

trait AsyncTaskExecutor{
    def submit(task: AsyncTask): Unit
}
