package com.github.skozlov.async.task;

import com.github.skozlov.async.deadline.Deadline;
import lombok.NonNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class TestTask extends Task {
  private final AtomicInteger count;
  private final Lock lock;
  private boolean shouldPause;
  private boolean paused;
  private final Condition pausedCondition;
  private boolean canResume;
  private final Condition canResumeCondition;

  public TestTask(@NonNull Deadline deadline) {
    super(deadline);
    count = new AtomicInteger(0);
    lock = new ReentrantLock();
    shouldPause = false;
    paused = false;
    pausedCondition = lock.newCondition();
    canResume = false;
    canResumeCondition = lock.newCondition();
  }

  @Override
  protected void executeImpl() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      if (shouldPause) {
        paused = true;
        pausedCondition.signalAll();
        while (!canResume) {
          canResumeCondition.await();
        }
      }
    } finally {
      lock.unlock();
    }
    count.getAndIncrement();
  }

  public int getCount() {
    return count.get();
  }

  public void pause() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      shouldPause = true;
    } finally {
      lock.unlock();
    }
  }

  public void awaitPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (!paused) {
        pausedCondition.await();
      }
    } finally {
      lock.unlock();
    }
  }

  public void resume() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      canResume = true;
      canResumeCondition.signalAll();
    } finally {
      lock.unlock();
    }
  }
}
