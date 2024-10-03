/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.model;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BoundedRingHashSet {

  private static final int DEFAULT_CAPACITY = 500_000;

  private int listSize;
  private int currentPosition = 0;
  private Set<Integer> backingSet;
  private int[] positionLookup;

  public BoundedRingHashSet(int listSize) {
    if (listSize <= 0 || listSize > 500_000) {
      throw new IllegalArgumentException("listSize must be between 1 and 500_000");
    }
    this.listSize = listSize;
    backingSet = new HashSet<>(listSize);
    positionLookup = new int[listSize];
  }

  public BoundedRingHashSet() {
    this(DEFAULT_CAPACITY);
  }

  public void add(int sessionHash) {
    if (!backingSet.contains(sessionHash)) {
      int position = getNextAvailablePosition();
      Optional.ofNullable(positionLookup[position])
          .ifPresent(value -> backingSet.remove(value));
      positionLookup[position] = sessionHash;
      backingSet.add(sessionHash);
    }
  }

  public void remove(int sessionHash) {
    backingSet.remove(sessionHash);
  }

  public boolean contains(int sessionHash) {
    return backingSet.contains(sessionHash);
  }

  private synchronized Integer getNextAvailablePosition() {
    if (currentPosition >= listSize) {
      currentPosition = 0;
    }
    return currentPosition++;
  }
}
