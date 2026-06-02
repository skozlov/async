package com.github.skozlov.commons.test;

import com.github.skozlov.commons.CheckedConsumer;
import com.github.skozlov.commons.CheckedRunnable;
import lombok.NonNull;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;

public class ThreadTestUtils {
  public static <E extends Exception> void withThread(
      @NonNull CheckedRunnable<? extends Exception> newThreadAction,
      @NonNull CheckedConsumer<Thread, ? extends E> actionWithNewThread
  ) throws E {
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
