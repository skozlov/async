package com.github.skozlov.commons.test;

import lombok.NonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNull;

public class ThreadTestUtils {
  public static void withThread(@NonNull Runnable newThreadAction, @NonNull Consumer<Thread> actionWithNewThread) {
    var threadError = new AtomicReference<Throwable>(null);
    var thread = new Thread(() -> {
      try {
        newThreadAction.run();
      } catch (Throwable e) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
        threadError.set(e);
      }
    });
    actionWithNewThread.accept(thread);
    assertNull(threadError.get());
  }
}
