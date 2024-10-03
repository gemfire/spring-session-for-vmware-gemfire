/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.support;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.Region;
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.events.SessionChangedEvent;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

public class SessionUtils {

  public static boolean isValidSessionId(@Nullable Object sessionId) {

    return Optional.ofNullable(sessionId)
        .map(Object::toString)
        .filter(StringUtils::hasText)
        .isPresent();
  }

  /**
   * Constructs a new {@link SessionCreatedEvent} initialized with the given {@link Session},
   * using the {@link AbstractGemFireOperationsSessionRepository SessionRepository} as the event source.
   *
   * @param session {@link Session} that is the subject of the {@link AbstractSessionEvent event}.
   * @return a new {@link SessionCreatedEvent}.
   * @see SessionCreatedEvent
   * @see Session
   */
  public static SessionCreatedEvent newSessionCreatedEvent(AbstractGemFireOperationsSessionRepository repository, Session session) {
    return new SessionCreatedEvent(repository, session);
  }

  /**
   * Constructs a new {@link SessionChangedEvent} initialized with the given {@link Session},
   * using the {@link AbstractGemFireOperationsSessionRepository SessionRepository} as the event source.
   *
   * @param session {@link Session} that is the subject of the {@link ApplicationEvent change event}.
   * @return a new {@link SessionChangedEvent}.
   * @see SessionChangedEvent
   * @see Session
   */
  public static SessionChangedEvent newSessionChangedEvent(AbstractGemFireOperationsSessionRepository repository, Session session) {
    return new SessionChangedEvent(repository, session);
  }

  /**
   * Constructs a new {@link SessionDeletedEvent} initialized with the given {@link Session},
   * using the {@link AbstractGemFireOperationsSessionRepository SessionRepository} as the event source.
   *
   * @param session {@link Session} that is the subject of the {@link AbstractSessionEvent event}.
   * @return a new {@link SessionDeletedEvent}.
   * @see SessionDeletedEvent
   * @see Session
   */
  public static SessionDeletedEvent newSessionDeletedEvent(AbstractGemFireOperationsSessionRepository repository, Session session) {
    return new SessionDeletedEvent(repository, session);
  }

  /**
   * Constructs a new {@link SessionDestroyedEvent} initialized with the given {@link Session},
   * using the {@link AbstractGemFireOperationsSessionRepository SessionRepository} as the event source.
   *
   * @param session {@link Session} that is the subject of the {@link AbstractSessionEvent event}.
   * @return a new {@link SessionDestroyedEvent}.
   * @see SessionDestroyedEvent
   * @see Session
   */
  public static SessionDestroyedEvent newSessionDestroyedEvent(AbstractGemFireOperationsSessionRepository repository, Session session) {
    return new SessionDestroyedEvent(repository, session);
  }

  /**
   * Constructs a new {@link SessionExpiredEvent} initialized with the given {@link Session},
   * using the {@link AbstractGemFireOperationsSessionRepository SessionRepository} as the event source.
   *
   * @param session {@link Session} that is the subject of the {@link AbstractSessionEvent event}.
   * @return a new {@link SessionExpiredEvent}.
   * @see SessionExpiredEvent
   * @see Session
   */
  public static SessionExpiredEvent newSessionExpiredEvent(AbstractGemFireOperationsSessionRepository repository, Session session) {
    return new SessionExpiredEvent(repository, session);
  }

  /**
   * Null-safe operation to determine whether the {@link Region} {@link EntryEvent} is
   * a {@link Operation#LOCAL_LOAD_CREATE} or a {@link Operation#LOCAL_LOAD_UPDATE}.
   *
   * @param event {@link Region} {@link EntryEvent} to evaluate.
   * @return a boolean value indicating whether the {@link Region} {@link EntryEvent} is a local load based event.
   * @see EntryEvent
   * @see #isNotLocalLoadEvent(EntryEvent)
   */
  public static boolean isLocalLoadEvent(@Nullable EntryEvent<?, ?> event) {
    return event != null && event.getOperation() != null && event.getOperation().isLocalLoad();
  }

  /**
   * Null-safe operation to determine whether the {@link Region} {@link EntryEvent} is
   * a {@link Operation#LOCAL_LOAD_CREATE} or a {@link Operation#LOCAL_LOAD_UPDATE}.
   *
   * @param event {@link Region} {@link EntryEvent} to evaluate.
   * @return a boolean value indicating whether the {@link Region} {@link EntryEvent} is a local load based event.
   * @see EntryEvent
   * @see #isLocalLoadEvent(EntryEvent)
   */
  public static boolean isNotLocalLoadEvent(@Nullable EntryEvent<?, ?> event) {
    return !isLocalLoadEvent(event);
  }

  /**
   * Determines whether the {@link EntryEvent#getNewValue() new value} contained in the {@link EntryEvent}
   * is a {@link Session}.
   *
   * @param entryEvent {@link EntryEvent} to evaluate.
   * @return a boolean value indicating whether the {@link EntryEvent#getNewValue() new value}
   * contained in the {@link EntryEvent} is a {@link Session}.
   * @see Session
   * @see EntryEvent
   */
  public static boolean isSession(EntryEvent<?, ?> entryEvent) {

    return Optional.ofNullable(entryEvent)
        .map(EntryEvent::getNewValue)
        .filter(Session.class::isInstance)
        .isPresent();
  }

  /**
   * Determines whether the given {@link Object} is a {@link Session}.
   *
   * @param target {@link Object} to evaluate.
   * @return a boolean value determining whether the given {@link Object} is a {@link Session}.
   * @see Session
   */
  public static boolean isSession(Object target) {
    return target instanceof Session;
  }

  /**
   * Casts the given {@link Object} into a {@link Session} iff the {@link Object} is a {@link Session}.
   * <p>
   * Otherwise, this method attempts to use the supplied {@link String Session ID} to create a {@link Session}
   * representation containing only the ID.
   *
   * @param event {@link EntryEvent}
   * @return a {@link Session} from the given {@link Object} or a {@link Session} representation
   * containing only the supplied {@link String Session ID}.
   * @throws IllegalStateException if the given {@link Object} is not a {@link Session}
   *                               and a {@link String Session ID} was not supplied.
   * @see Session
   * @see #isSession(Object)
   */
  public static Session toSession(EntryEvent<Object, Session> event) {

    return toSession(event.getNewValue(), event.getKey());
  }

  public static Session toSession(@Nullable Object target, Object sessionId) {

    return isSession(target)
        ? (Session) target
        : Optional.ofNullable(sessionId)
        .filter(SessionUtils::isValidSessionId)
        .map(Object::toString)
        .map(SessionIdHolder::create)
        .orElseThrow(() -> newIllegalStateException(
            "The Session or the Session ID [%s] must be known to trigger a Session event", sessionId));
  }
}
