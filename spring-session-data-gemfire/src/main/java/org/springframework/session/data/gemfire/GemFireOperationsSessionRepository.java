/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import org.apache.geode.cache.query.SelectResults;
import org.springframework.data.gemfire.GemfireOperations;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link GemFireOperationsSessionRepository} class is a Spring {@link SessionRepository} implementation
 * that interfaces with and uses Apache Geode or Pivotal GemFire to back and store Spring Sessions.
 *
 * @author John Blum
 * @see GemfireOperations
 * @see Session
 * @see SessionRepository
 * @see AbstractGemFireOperationsSessionRepository
 * @since 1.1.0
 */
public class GemFireOperationsSessionRepository extends AbstractGemFireOperationsSessionRepository {

  // Pivotal GemFire OQL query used to lookup Sessions by arbitrary attributes.
  protected static final String FIND_SESSIONS_BY_INDEX_NAME_AND_INDEX_VALUE_QUERY =
      "SELECT s FROM %1$s s WHERE s.attributes['%2$s'] = $1";

  // Pivotal GemFire OQL query used to look up Sessions by principal name.
  protected static final String FIND_SESSIONS_BY_PRINCIPAL_NAME_QUERY =
      "SELECT s FROM %1$s s WHERE s.principalName = $1";

  /**
   * Constructs a new instance of {@link GemFireOperationsSessionRepository} initialized with
   * the required {@link GemfireOperations} object used to perform data access operations
   * for managing (HTTP) {@link Session} state.
   *
   * @param template {@link GemfireOperations} object used to access and manage {@link Session} state in GemFire.
   * @see GemfireOperations
   */
  public GemFireOperationsSessionRepository(GemfireOperations template) {
    super(template);
  }

  /**
   * Constructs a new {@link Session} instance backed by GemFire.
   *
   * @return an instance of {@link Session} backed by GemFire.
   * @see GemFireSession#create()
   * @see Session
   * @see #configure(Session)
   */
  @NonNull
  public Session createSession() {
    return configure(GemFireSession.create());
  }

  /**
   * Finds an existing, non-expired {@link Session} by ID.
   * <p>
   * If the {@link Session} is expired, then the {@link Session} is deleted and {@literal null} is returned.
   *
   * @param sessionId {@link String} containing the {@link Session#getId() ID}} of the {@link Session} to get.
   * @return an existing {@link Session} by ID or {@literal null} if no {@link Session} exists
   * or the {@link Session} expired.
   * @see GemFireSession#from(Session)
   * @see org.springframework.data.gemfire.GemfireTemplate#get(Object)
   * @see Session
   * @see #getSessionsTemplate()
   * @see #prepare(Session)
   * @see #delete(Session)
   */
  @Nullable
  public Session findById(String sessionId) {

    return Optional.ofNullable(getSessionsTemplate().get(sessionId))
        .map(session -> ((Session) session).isExpired()
            ? delete((Session) session)
            : prepare(GemFireSession.from((Session) session)))
        .orElse(null);
  }

  /**
   * Finds all available {@link Session Sessions} with the particular attribute indexed by {@link String name}
   * having the given {@link Object value}.
   *
   * @param indexName  {@link String name} of the indexed {@link Session} attribute.
   *                   (e.g. {@link org.springframework.session.FindByIndexNameSessionRepository#PRINCIPAL_NAME_INDEX_NAME}).
   * @param indexValue {@link Object value} of the indexed {@link Session} attribute to search on
   *                   (e.g. {@literal username}).
   * @return a mapping of {@link Session#getId()} Session IDs} to {@link Session} objects.
   * @see org.springframework.data.gemfire.GemfireTemplate#find(String, Object...)
   * @see Session
   * @see Map
   * @see #getSessionsTemplate()
   * @see #prepareQuery(String)
   * @see #prepare(Session)
   */
  @Override
  public Map<String, Session> findByIndexNameAndIndexValue(String indexName, String indexValue) {

    SelectResults<Session> results = getSessionsTemplate().find(prepareQuery(indexName), indexValue);

    Map<String, Session> sessions = new HashMap<>(results.size());

    results.asList().forEach(session ->
        sessions.put(session.getId(), prepare(session)));

    return sessions;
  }

  /**
   * Prepares the appropriate Pivotal GemFire OQL query based on the indexed Session attribute
   * name.
   *
   * @param indexName a String indicating the name of the indexed Session attribute.
   * @return an appropriate Pivotal GemFire OQL statement for querying on a particular indexed
   * Session attribute.
   * @see #getSessionsRegionName()
   */
  protected String prepareQuery(String indexName) {

    String fullyQualifiedRegionName = getSessionsRegionName();

    return PRINCIPAL_NAME_INDEX_NAME.equals(indexName)
        ? String.format(FIND_SESSIONS_BY_PRINCIPAL_NAME_QUERY, fullyQualifiedRegionName)
        : String.format(FIND_SESSIONS_BY_INDEX_NAME_AND_INDEX_VALUE_QUERY, fullyQualifiedRegionName, indexName);
  }

  /**
   * Prepares the (loaded) {@link Session} for use.
   *
   * @param session {@link Session} to prepare.
   * @return the prepared {@link Session}.
   * @see Session
   * @see #configure(Session)
   * @see #commit(Session)
   * @see #touch(Session)
   */
  private Session prepare(Session session) {
    return touch(commit(configure(session)));
  }

  /**
   * Saves the specified {@link Session} to Apache Geode or Pivotal GemFire.
   * <p>
   * Warning, the save method should never be called asynchronously and concurrently, from a separate Thread,
   * while the caller continues to modify the given {@link Session} from the forking Thread
   * or data loss can occur!  There is a reason why this method is blocking!
   *
   * @param session the {@link Session} to save.
   * @see GemfireOperations#put(Object, Object)
   * @see Session
   * @see #isNonNullAndDirty(Session)
   * @see #doSave(Session)
   */
  public void save(@Nullable Session session) {

    if (isNonNullAndDirty(session)) {
      doSave(session);
    }
  }

  /**
   * Determines whether the given {@link Session} is dirty (i.e. has any changes).
   *
   * @param session {@link Session} to evaluate.
   * @return a boolean value indicating whether the {@link Session} is dirty or not.
   * @see GemFireSession#hasDelta()
   * @see Session
   */
  private boolean isDirty(@NonNull Session session) {
    return !(session instanceof GemFireSession) || ((GemFireSession<?>) session).hasDelta();
  }

  /**
   * Determines whether the given {@link Session} is {@literal non-null} and {@link #isDirty(Session) dirty}.
   *
   * @param session {@link Session} to evaluate.
   * @return a boolean value indicating whether the given {@link Session} is {@literal non-null}
   * and {@link #isDirty(Session) dirty}.
   * @see #isDirty(Session)
   */
  private boolean isNonNullAndDirty(@Nullable Session session) {
    return Objects.nonNull(session) && isDirty(session);
  }

  /**
   * Performs the actual {@link Session} save operation, persisting the {@link Session} state to eitehr Apache Geode
   * or Pivotal GemFire!
   *
   * @param session {@link Session} to save.
   * @see org.springframework.data.gemfire.GemfireTemplate#put(Object, Object)
   * @see Session
   * @see #commit(Session)
   */
  void doSave(@NonNull Session session) {

    // Save Session As GemFireSession
    getSessionsTemplate().put(session.getId(), GemFireSession.from(session));

    // Commit Session
    commit(session);
  }

  /**
   * Deletes (removes) any existing {@link Session} from GemFire. This operation
   * also results in a SessionDeletedEvent.
   *
   * @param sessionId a String indicating the ID of the Session to remove from GemFire.
   * @see GemfireOperations#remove(Object)
   * @see #handleDeleted(String, Session)
   */
  public void deleteById(String sessionId) {
    Session sessionBeforeRemoval = getSessionsTemplate().get(sessionId);
    Session removedSession = getSessionsTemplate().<Object, Session>remove(sessionId);
    Session session = removedSession != null ? removedSession : sessionBeforeRemoval;
    handleDeleted(sessionId, session);
  }
}
