/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.NonNull;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.events.SessionChangedEvent;
import org.springframework.session.data.gemfire.model.BoundedRingHashSet;
import org.springframework.session.data.gemfire.support.SessionUtils;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

public class SessionEventHandlerCacheListenerAdapter extends CacheListenerAdapter<Object, Session> {

  private final AbstractGemFireOperationsSessionRepository sessionRepository;

  private final BoundedRingHashSet cachedSessionIds;

  /**
   * Constructs a new instance of the {@link SessionEventHandlerCacheListenerAdapter} initialized with
   * the given {@link AbstractGemFireOperationsSessionRepository}.
   *
   * @param sessionRepository {@link AbstractGemFireOperationsSessionRepository} used by this event handler
   *                          to manage {@link AbstractSessionEvent Session Events}.
   * @throws IllegalArgumentException if {@link AbstractGemFireOperationsSessionRepository} is {@literal null}.
   * @see AbstractGemFireOperationsSessionRepository
   */
  protected SessionEventHandlerCacheListenerAdapter(
      AbstractGemFireOperationsSessionRepository sessionRepository, BoundedRingHashSet cachedSessionIds) {

    Assert.notNull(sessionRepository, "SessionRepository is required");

    this.sessionRepository = sessionRepository;
    this.cachedSessionIds = cachedSessionIds;
  }

  /**
   * Returns a reference to the configured {@link SessionRepository}.
   *
   * @return a reference to the configured {@link SessionRepository}.
   * @see AbstractGemFireOperationsSessionRepository
   */
  protected @NonNull AbstractGemFireOperationsSessionRepository getSessionRepository() {
    return this.sessionRepository;
  }

  /**
   * Causes Session deleted events to be published to the Spring application context.
   *
   * @param sessionId a String indicating the ID of the Session.
   * @param session   a reference to the Session triggering the event.
   * @see SessionDeletedEvent
   * @see Session
   * @see SessionUtils#newSessionDeletedEvent(AbstractGemFireOperationsSessionRepository, Session)
   * @see AbstractGemFireOperationsSessionRepository#publishEvent(ApplicationEvent)
   * @see SessionUtils#toSession(Object, Object)
   */
  protected void afterDelete(@NonNull String sessionId, @NonNull Session session) {
    if (sessionId == null || (session != null && session.getId() == null)) {
      throw new IllegalStateException(String.format("The Session or the Session ID [%s] must be known to trigger a Session event", sessionId));
    }
    getSessionRepository().publishEvent(SessionUtils.newSessionDeletedEvent(getSessionRepository(), SessionUtils.toSession(session, sessionId)));
    cachedSessionIds.remove(ObjectUtils.nullSafeHashCode(sessionId));
  }

  /**
   * Callback method triggered when an entry is destroyed (removed) in the {@link Session} cache {@link Region}.
   *
   * @param event {@link EntryEvent} containing the details of the cache operation.
   * @see SessionDestroyedEvent
   * @see Session
   * @see EntryEvent
   * @see SessionUtils#newSessionDestroyedEvent(AbstractGemFireOperationsSessionRepository, Session)
   * @see AbstractGemFireOperationsSessionRepository#publishEvent(ApplicationEvent)
   * @see SessionUtils#toSession(Object, Object)
   */
  @Override
  public void afterDestroy(EntryEvent<Object, Session> event) {
    Optional.ofNullable(event).ifPresent(entryEvent -> {
      Object sessionId = entryEvent.getKey();
      getSessionRepository()
          .publishEvent(SessionUtils.newSessionDestroyedEvent(getSessionRepository(), SessionUtils.toSession(entryEvent.getOldValue(), sessionId)));
      cachedSessionIds.remove(ObjectUtils.nullSafeHashCode(sessionId));
    });
  }

  /**
   * Callback method triggered when an entry is invalidated (expired) in the {@link Session} cache {@link Region}.
   *
   * @param event {@link EntryEvent} containing the details of the cache operation.
   * @see SessionExpiredEvent
   * @see Session
   * @see EntryEvent
   * @see SessionUtils ::newSessionExpiredEvent(Session)
   * @see AbstractGemFireOperationsSessionRepository#publishEvent(ApplicationEvent)
   * @see SessionUtils ::toSession(Object, Object)
   */
  @Override
  public void afterInvalidate(EntryEvent<Object, Session> event) {
    Optional.ofNullable(event).ifPresent(entryEvent -> {
      Object key = event.getKey();
      getSessionRepository()
          .publishEvent(SessionUtils.newSessionExpiredEvent(getSessionRepository(), SessionUtils.toSession(event.getOldValue(), key)));
      cachedSessionIds.remove(ObjectUtils.nullSafeHashCode(key));
    });
  }

  public void afterExpired(@NonNull String sessionId, @NonNull Session session){
    if (sessionId == null || (session != null && session.getId() == null)) {
      throw new IllegalStateException(String.format("The Session or the Session ID [%s] must be known to trigger a Session event", sessionId));
    }
    getSessionRepository().publishEvent(SessionUtils.newSessionExpiredEvent(getSessionRepository(), SessionUtils.toSession(session, sessionId)));
    cachedSessionIds.remove(ObjectUtils.nullSafeHashCode(sessionId));
  }

  /**
   * Callback method triggered when an entry is updated in the {@link Session} cache {@link Region}.
   *
   * @param event {@link EntryEvent} containing the details of the cache operation.
   * @see SessionChangedEvent
   * @see Session
   * @see EntryEvent
   * @see SessionUtils ::newSessionChangedEvent(Session)
   * @see AbstractGemFireOperationsSessionRepository#publishEvent(ApplicationEvent)
   * @see SessionUtils ::toSession(Object, Object)
   */
  @Override
  public void afterUpdate(EntryEvent<Object, Session> event) {
    Optional.ofNullable(event).ifPresent(entryEvent ->
        getSessionRepository()
            .publishEvent(SessionUtils.newSessionChangedEvent(getSessionRepository(), SessionUtils.toSession(event.getNewValue(), event.getKey()))));
  }
}
