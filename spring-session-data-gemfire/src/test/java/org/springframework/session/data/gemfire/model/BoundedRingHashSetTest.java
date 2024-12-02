/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.model;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BoundedRingHashSetTest {

  @Test
  public void testMapSizeNeverExceedMaximum() {
    BoundedRingHashSet ringHashSet = new BoundedRingHashSet(100);

    for (int i = 0; i < 1_000_000; i++) {
      ringHashSet.add(i);
    }

    Assertions.assertThat(ringHashSet.getSize()).isEqualTo(100);
  }

  @Test
  public void testGetNextPosition() {
    BoundedRingHashSet ringHashSet = new BoundedRingHashSet(3);

    Assertions.assertThat(ringHashSet.getNextAvailablePosition()).isEqualTo(0);
    Assertions.assertThat(ringHashSet.getNextAvailablePosition()).isEqualTo(1);
    Assertions.assertThat(ringHashSet.getNextAvailablePosition()).isEqualTo(2);
    Assertions.assertThat(ringHashSet.getNextAvailablePosition()).isEqualTo(0);
  }

  @Test
  public void testSynchronization() throws InterruptedException, ExecutionException, TimeoutException {
    BoundedRingHashSet ringHashSet = new BoundedRingHashSet(100);

    ExecutorService executorService = Executors.newFixedThreadPool(200);

    LinkedList<Future<Boolean>> callables = new LinkedList<>();

    for (int i = 0; i < 2000; i++) {
      callables.add(executorService.submit(() -> {
        for (int j = 0; j < 100; j++) {
          for (int i1 = 0; i1 < 10_000; i1++) {
            try {
              ringHashSet.add(i1);
            } catch (Exception e) {
              e.printStackTrace();
              return false;
            }
          }
          return true;
        }
        return true;
      }));
    }

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.SECONDS);
    List<Runnable> runnables = executorService.shutdownNow();

    Assertions.assertThat(runnables).hasSize(0);

    for (Iterator<Future<Boolean>> iterator = callables.iterator(); iterator.hasNext(); ) {
      Future<Boolean> next = iterator.next();
      Assertions.assertThat(next.get(2, TimeUnit.SECONDS)).isTrue();
    }
  }
}
