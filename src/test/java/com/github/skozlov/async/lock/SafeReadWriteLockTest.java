package com.github.skozlov.async.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SafeReadWriteLockTest {
  private Lock readMock;
  private Lock writeMock;
  private SafeReadWriteLock TOT;

  @BeforeEach
  void init() {
    readMock = mock(Lock.class);
    writeMock = mock(Lock.class);
    ReadWriteLock unsafe = new ReadWriteLock() {
      @Override
      public Lock readLock() {
        return readMock;
      }

      @Override
      public Lock writeLock() {
        return writeMock;
      }
    };
    TOT = SafeReadWriteLock.from(unsafe);
  }

  @Test
  void readLockTest() {
    assertThat(TOT.readLock().toUnsafe()).isSameAs(readMock);
  }

  @Test
  void writeLockTest() {
    assertThat(TOT.writeLock().toUnsafe()).isSameAs(writeMock);
  }
}
