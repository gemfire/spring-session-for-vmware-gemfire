/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire;

import org.apache.geode.cache.CacheWriterException;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.util.CacheWriterAdapter;
import org.springframework.lang.NonNull;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.model.BoundedRingHashSet;
import org.springframework.session.data.gemfire.support.SessionUtils;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

public class SessionEventHandlerCacheWriterAdapter extends CacheWriterAdapter<Object, Session> {
  private final AbstractGemFireOperationsSessionRepository sessionRepository;

  private BoundedRingHashSet cachedSessionIds = new BoundedRingHashSet();

  protected SessionEventHandlerCacheWriterAdapter() {
    this(null, null);
  }

  /**
   * Constructs a new instance of the {@link SessionEventHandlerCacheWriterAdapter} initialized with
   * the given {@link AbstractGemFireOperationsSessionRepository}.
   *
   * @param sessionRepository {@link AbstractGemFireOperationsSessionRepository} used by this event handler
   *                          to manage {@link AbstractSessionEvent Session Events}.
   * @throws IllegalArgumentException if {@link AbstractGemFireOperationsSessionRepository} is {@literal null}.
   * @see AbstractGemFireOperationsSessionRepository
   */
  protected SessionEventHandlerCacheWriterAdapter(
      AbstractGemFireOperationsSessionRepository sessionRepository, BoundedRingHashSet cachedSessionIds) {

//    Assert.notNull(sessionRepository, "SessionRepository is required");

    this.sessionRepository = sessionRepository;
    this.cachedSessionIds = cachedSessionIds;
  }

  @Override
  public void beforeCreate(EntryEvent<Object, Session> event) throws CacheWriterException {
    Optional.ofNullable(event).filter(this::isSession).map(sessionEntryEvent -> sessionEntryEvent.getNewValue()).ifPresent(session -> {
      if (isNotLocalLoadEvent(event)) {
        int hashedSessionId = ObjectUtils.nullSafeHashCode(event.getKey());
        if (!getCachedSessionIds().contains(hashedSessionId)) {
          getSessionRepository()
              .publishEvent(SessionUtils.newSessionCreatedEvent(getSessionRepository(), SessionUtils.toSession(event)));
          getCachedSessionIds().add(hashedSessionId);
        }
      }
    });
  }

  protected boolean isSession(EntryEvent<?, ?> entryEvent) {

    return Optional.ofNullable(entryEvent)
        .map(EntryEvent::getNewValue)
        .filter(Session.class::isInstance)
        .isPresent();
  }

  public BoundedRingHashSet getCachedSessionIds() {
    return cachedSessionIds;
  }

  protected boolean isNotLocalLoadEvent(EntryEvent<Object, Session> event) {
    return SessionUtils.isNotLocalLoadEvent(event);
  }

//  private boolean isProxiedRegion(Region<Object, Session> region) {
//    return region instanceof LocalRegion &&
//        (((LocalRegion) region).getDataPolicy() == DataPolicy.NORMAL || ((LocalRegion) region).getDataPolicy() == DataPolicy.EMPTY)
//        && ((LocalRegion) region).getScope() == Scope.LOCAL
//        && StringUtils.hasText(((LocalRegion) region).getPoolName());
//  }

  /**
   * Returns a reference to the configured {@link SessionRepository}.
   *
   * @return a reference to the configured {@link SessionRepository}.
   * @see AbstractGemFireOperationsSessionRepository
   */
  protected @NonNull AbstractGemFireOperationsSessionRepository getSessionRepository() {
    return this.sessionRepository;
  }
}
