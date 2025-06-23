package com.github.skozlov.async.collection;

import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.deadline.DeadlinePassedException;
import com.github.skozlov.async.lock.SafeCondition;
import com.github.skozlov.async.lock.SafeLock;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.min;

public class LinkedQueueWithDeadline<E> implements QueueWithDeadline<E> {
  private final int capacity;
  private final SafeLock enqueueLock;
  private final SafeLock dequeueLock;
  private final SafeLock commonLock;
  private final SafeCondition nonEmptyCondition;
  private final SafeCondition nonFullCondition;
  private int size;
  private Node<E> head;
  private Node<E> tail;

  public LinkedQueueWithDeadline(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Non-positive capacity: " + capacity);
    }
    this.capacity = capacity;
    enqueueLock = SafeLock.from(new ReentrantLock());
    dequeueLock = SafeLock.from(new ReentrantLock());
    commonLock = SafeLock.from(new ReentrantLock());
    nonEmptyCondition = commonLock.newCondition();
    nonFullCondition = commonLock.newCondition();
    size = 0;
    head = null;
    tail = null;
  }

  @Override
  public void enqueue(@NonNull Iterable<? extends E> elements, @NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException {
    Iterator<? extends E> it = elements.iterator();
    if (!it.hasNext()) {
      return;
    }
    try (var ignored = enqueueLock.lock(deadline)) {
      try (var ignored1 = commonLock.lock(deadline)) {
        do {
          nonFullCondition.await(deadline, () -> size < capacity);
          int numCanAdd = capacity - size;
          Node<E> newElementsHead = new Node<>(it.next());
          Node<E> newElementsTail = newElementsHead;
          int numAdded = 1;
          for (; numAdded < numCanAdd && it.hasNext(); numAdded++) {
            newElementsTail.next = new Node<>(it.next());
            newElementsTail = newElementsTail.next;
          }
          if (tail == null) {
            head = newElementsHead;
          } else {
            tail.next = newElementsHead;
          }
          tail = newElementsTail;
          size += numAdded;
          nonEmptyCondition.signalAll();
        } while (it.hasNext());
      }
    }
  }

  @Override
  public @NonNull List<E> dequeue(int numElements, @NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException {
    if (numElements <= 0) {
      throw new IllegalArgumentException("Non-positive numElements: " + numElements);
    }
    List<E> results = new ArrayList<>(numElements);
    try (var ignored = dequeueLock.lock(deadline)) {
      try (var ignored1 = commonLock.lock(deadline)) {
        do {
          nonEmptyCondition.await(deadline, () -> size > 0);
          int numToRemove = min(size, numElements - results.size());
          for (int i = 0; i < numToRemove; i++) {
            results.add(head.value);
            head = head.next;
          }
          if (head == null) {
            tail = null;
          }
          size -= numToRemove;
          nonFullCondition.signalAll();
        } while (results.size() < numElements);
      }
    }
    return results;
  }

  @RequiredArgsConstructor
  private static class Node<E> {
    public final @NonNull E value;

    public Node<E> next;
  }
}
