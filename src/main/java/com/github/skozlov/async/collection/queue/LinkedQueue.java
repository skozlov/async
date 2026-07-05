package com.github.skozlov.async.collection.queue;

import com.github.skozlov.async.collection.Try;
import com.github.skozlov.async.deadline.Deadline;
import com.github.skozlov.async.lock.LockAutoClose;
import com.github.skozlov.async.lock.SafeLock;
import com.github.skozlov.async.lock.condition.SafeCondition;
import com.github.skozlov.commons.CheckedSupplier;
import com.github.skozlov.commons.Pair;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

public class LinkedQueue<E> implements Queue<E> {
  private final int capacity;
  private final SafeLock lock;
  private final SafeCondition nonEmptyCondition;
  private final SafeCondition nonFullCondition;
  private final QueueListener listener;
  private int size;
  private Node<E> head;
  private Node<E> tail;

  public LinkedQueue(int capacity, @NonNull QueueListener listener) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Non-positive capacity: " + capacity);
    }
    this.capacity = capacity;
    this.listener = NonThrowingQueueListener.from(listener);
    lock = SafeLock.from(new ReentrantLock());
    nonEmptyCondition = lock.newCondition();
    nonFullCondition = lock.newCondition();
    size = 0;
    head = null;
    tail = null;
  }

  @Override
  public @NonNull Try<Pair<Integer, Iterator<? extends E>>> enqueue(@NonNull Iterable<? extends E> elements, @NonNull Deadline deadline) {
    int added = 0;
    Iterator<? extends E> it = elements.iterator();
    try {
      if (!it.hasNext()) {
        return Try.success(new Pair<>(added, it));
      }
      try (LockAutoClose ignored = lock.lock(deadline)) {
        do {
          CheckedSupplier<Boolean, InterruptedException> hasFreeSpace = () -> size < capacity;
          if (!hasFreeSpace.get()) {
            listener.onWaitingForFreeSpace(deadline);
            nonFullCondition.await(deadline, hasFreeSpace);
          }
          Node<E> newPartHead = new Node<>(it.next());
          Node<E> newPartTail = newPartHead;
          int newPartSize = 1;
          for (int canAdd = capacity - size - 1; canAdd > 0 && it.hasNext(); canAdd--, newPartSize++) {
            newPartTail.next = new Node<>(it.next());
            newPartTail = newPartTail.next;
          }
          if (head == null) {
            head = newPartHead;
          } else {
            tail.next = newPartHead;
          }
          tail = newPartTail;
          size += newPartSize;
          added += newPartSize;
          listener.onEnqueuedElements(newPartSize, size);
          nonEmptyCondition.signalAll();
        } while (it.hasNext());
      }
      return Try.success(new Pair<>(added, it));
    } catch (InterruptedException | RuntimeException e) {
      return Try.failure(e, new Pair<>(added, it));
    }
  }

  @Override
  public @NonNull Try<List<E>> dequeue(int minElements, int maxElements, @NonNull Deadline deadline) {
    if (minElements < 0) {
      throw new IllegalArgumentException("Negative minElements: " + minElements);
    }
    if (maxElements < minElements) {
      throw new IllegalArgumentException(format("maxElements (%d) < minElements (%d)", maxElements, minElements));
    }
    if (maxElements == 0) {
      return Try.success(emptyList());
    }
    List<E> result = new ArrayList<>(minElements);
    try (LockAutoClose ignored = lock.lock(deadline)) {
      do {
        if (size == 0) {
          if (result.size() >= minElements) {
            break;
          } else {
            CheckedSupplier<Boolean, InterruptedException> isNotEmpty = () -> size > 0;
            if (!isNotEmpty.get()) {
              listener.onWaitingForElements(deadline);
              nonEmptyCondition.await(deadline, isNotEmpty);
            }
          }
        }
        int dequeued = 0;
        while (head != null && result.size() < maxElements) {
          result.add(head.value);
          head = head.next;
          dequeued++;
        }
        size -= dequeued;
        if (head == null) {
          tail = null;
        }
        listener.onDequeuedElements(dequeued, size);
        nonFullCondition.signalAll();
      } while (result.size() < maxElements);
      return Try.success(result);
    } catch (InterruptedException | RuntimeException e) {
      return Try.failure(e, result);
    }
  }

  @RequiredArgsConstructor
  private static class Node<E> {
    public final @NonNull E value;

    public Node<E> next;
  }
}
