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

import static java.lang.String.format;
import static java.util.Collections.emptyList;

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
      do {
        int canAdd;
        try (var ignored1 = commonLock.lock(deadline)) {
          nonFullCondition.await(deadline, () -> size < capacity);
          canAdd = capacity - size;
        }
        Node<E> newPartHead = new Node<>(it.next());
        Node<E> newPartTail = newPartHead;
        int collected = 1;
        canAdd--;
        for (; canAdd > 0 && it.hasNext(); collected++, canAdd--) {
          newPartTail.next = new Node<>(it.next());
          newPartTail = newPartTail.next;
        }
        try (var ignored1 = commonLock.lock(deadline)) {
          if (head == null) {
            head = newPartHead;
          } else {
            tail.next = newPartHead;
          }
          tail = newPartTail;
          size += collected;
          nonEmptyCondition.signalAll();
        }
      } while (it.hasNext());
    }
  }

  @Override
  public @NonNull List<E> dequeue(int minElements, int maxElements, @NonNull Deadline deadline) throws DeadlinePassedException, InterruptedException {
    if (minElements < 0) {
      throw new IllegalArgumentException("Negative minElements: " + minElements);
    }
    if (maxElements < minElements) {
      throw new IllegalArgumentException(format("maxElements (%d) < minElements (%d)", maxElements, minElements));
    }
    if (maxElements == 0) {
      return emptyList();
    }
    List<E> results = new ArrayList<>(minElements);
    try (var ignored = dequeueLock.lock(deadline)) {
      do {
        try (var ignored1 = commonLock.lock(deadline)) {
          if (size == 0) {
            if (results.size() >= minElements) {
              return results;
            } else {
              nonEmptyCondition.await(deadline, () -> size > 0);
            }
          }
          for (; head != null && results.size() < maxElements; size--) {
            results.add(head.value);
            head = head.next;
            if (head == null) {
              tail = null;
            }
          }
          nonFullCondition.signalAll();
        }
      } while (results.size() < maxElements);
    }
    return results;
  }

  @RequiredArgsConstructor
  private static class Node<E> {
    public final @NonNull E value;

    public Node<E> next;
  }
}
