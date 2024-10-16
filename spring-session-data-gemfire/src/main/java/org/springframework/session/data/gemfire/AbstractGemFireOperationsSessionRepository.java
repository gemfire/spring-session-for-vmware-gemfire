/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import org.apache.geode.DataSerializable;
import org.apache.geode.DataSerializer;
import org.apache.geode.Delta;
import org.apache.geode.InvalidDeltaException;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.InterestResultPolicy;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.gemfire.GemfireAccessor;
import org.springframework.data.gemfire.GemfireOperations;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.data.gemfire.events.SessionChangedEvent;
import org.springframework.session.data.gemfire.model.BoundedRingHashSet;
import org.springframework.session.data.gemfire.support.GemFireUtils;
import org.springframework.session.data.gemfire.support.IsDirtyPredicate;
import org.springframework.session.data.gemfire.support.SessionIdHolder;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

/**
 * {@link AbstractGemFireOperationsSessionRepository} is an abstract base class encapsulating functionality
 * common to all implementations that support {@link SessionRepository} operations backed by Apache Geode.
 *
 * @author John Blum
 * @see Duration
 * @see Instant
 * @see UUID
 * @see DataSerializable
 * @see DataSerializer
 * @see Delta
 * @see EntryEvent
 * @see Operation
 * @see Region
 * @see RegionAttributes
 * @see Pool
 * @see PoolManager
 * @see CacheListenerAdapter
 * @see ApplicationEvent
 * @see ApplicationEventPublisher
 * @see ApplicationEventPublisherAware
 * @see GemfireOperations
 * @see FindByIndexNameSessionRepository
 * @see Session
 * @see SessionRepository
 * @see GemFireHttpSessionConfiguration
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession
 * @see SessionChangedEvent
 * @see org.springframework.session.data.gemfire.support.DeltaAwareDirtyPredicate
 * @see IsDirtyPredicate
 * @see SessionIdHolder
 * @see AbstractSessionEvent
 * @see SessionCreatedEvent
 * @see SessionDeletedEvent
 * @see SessionDestroyedEvent
 * @see SessionExpiredEvent
 * @since 1.1.0
 */
public abstract class AbstractGemFireOperationsSessionRepository
    implements ApplicationEventPublisherAware, FindByIndexNameSessionRepository<Session> {

  private static final boolean DEFAULT_CLIENT_SUBSCRIPTIONS_ENABLED = false;
  private static final boolean DEFAULT_REGISTER_INTEREST_DURABILITY = false;
  private static final boolean DEFAULT_REGISTER_INTEREST_ENABLED = false;
  private static final boolean DEFAULT_REGISTER_INTEREST_RECEIVE_VALUES = true;

  // TODO - use non-static variable
  private static final AtomicBoolean usingDataSerialization = new AtomicBoolean(false);

  private static final Duration DEFAULT_MAX_INACTIVE_INTERVAL =
      Duration.ofSeconds(GemFireHttpSessionConfiguration.DEFAULT_MAX_INACTIVE_INTERVAL_IN_SECONDS);

  private static final InterestResultPolicy DEFAULT_REGISTER_INTEREST_RESULT_POLICY = InterestResultPolicy.NONE;

  private static final IsDirtyPredicate DEFAULT_IS_DIRTY_PREDICATE =
      GemFireHttpSessionConfiguration.DEFAULT_IS_DIRTY_PREDICATE;

  private ApplicationEventPublisher applicationEventPublisher = event -> {
  };

  private Duration maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;

  private final GemfireOperations template;

  private IsDirtyPredicate dirtyPredicate = DEFAULT_IS_DIRTY_PREDICATE;

  private final Logger logger = newLogger();

  private final Region<Object, Session> sessions;

  private SessionEventHandlerCacheListenerAdapter sessionEventHandler;
  private SessionEventHandlerCacheWriterAdapter sessionEventHandlerCacheWriter;

  private final Set<Integer> interestingSessionIds = new ConcurrentSkipListSet<>();

  /**
   * Protected, default constructor used by extensions of {@link AbstractGemFireOperationsSessionRepository}
   * in order to affect and assess {@link SessionRepository} configuration and state.
   */
  protected AbstractGemFireOperationsSessionRepository() {

    this.sessions = null;
    this.template = null;
  }

  /**
   * Constructs a new instance of {@link AbstractGemFireOperationsSessionRepository} initialized with a required
   * {@link GemfireOperations} object, which is used to perform Apache Geode or Pivotal GemFire data access operations
   * on the cache {@link Region} storing and managing {@link Session} state to support this {@link SessionRepository}
   * and its operations.
   *
   * @param template {@link GemfireOperations} object used to interact with the Apache Geode or Pivotal GemFire
   *                 cache {@link Region} storing and managing {@link Session} state; must not be {@literal null}.
   * @throws IllegalArgumentException if {@link GemfireOperations} is {@literal null}.
   * @see GemfireOperations
   * @see #resolveSessionsRegion(GemfireOperations)
   * @see #initializeSessionsRegion(Region)
   * @see #newLogger()
   */
  public AbstractGemFireOperationsSessionRepository(GemfireOperations template) {

    Assert.notNull(template, "GemfireOperations is required");

    this.template = template;
    this.sessions = initializeSessionsRegion(resolveSessionsRegion(template));
  }

  /**
   * Resolves the cache {@link Region} used to store and manage {@link Session} state
   * from the given {@link GemfireOperations} object.
   *
   * @param gemfireOperations {@link GemfireOperations} object used to resolve the {@link Session} {@link Region}.
   * @return the resolve cache {@link Region} used to store and manage {@link Session} state.
   * @throws IllegalStateException if the {@link Session Sessions} {@link Region} could not be resolved.
   * @see GemfireOperations
   * @see Session
   * @see Region
   */
  private Region<Object, Session> resolveSessionsRegion(@Nullable GemfireOperations gemfireOperations) {

    return Optional.ofNullable(gemfireOperations)
        .filter(GemfireAccessor.class::isInstance)
        .map(GemfireAccessor.class::cast)
        .<Region<Object, Session>>map(GemfireAccessor::getRegion)
        .orElseThrow(() -> newIllegalStateException("The ClusteredSpringSessions Region could not be resolved"));
  }

  /**
   * Initializes the cache {@link Region} used to store and manage {@link Session} state and register this
   * {@link SessionRepository} as an Apache Geode / Pivotal GemFire {@link org.apache.geode.cache.CacheListener}.
   *
   * @param sessionsRegion {@link Region} to initialize.
   * @return the given {@link Region}.
   * @see Region
   * @see #newSessionEventHandler(BoundedRingHashSet)
   * @see #isRegionRegisterInterestAllowed(Region)
   */
  private @Nullable Region<Object, Session> initializeSessionsRegion(
      @Nullable Region<Object, Session> sessionsRegion) {

    Optional.ofNullable(sessionsRegion)
        .map(Region::getAttributesMutator)
        .ifPresent(sessionsRegionAttributesMutator -> {

          BoundedRingHashSet ringHashSet = new BoundedRingHashSet();
          this.sessionEventHandler = newSessionEventHandler(ringHashSet);
          this.sessionEventHandlerCacheWriter = newSessionEventHandlerCacheWriterAdapter(ringHashSet);

          sessionsRegionAttributesMutator.addCacheListener(this.sessionEventHandler);
          sessionsRegionAttributesMutator.setCacheWriter(sessionEventHandlerCacheWriter);
        });

    return sessionsRegion;
  }

  /**
   * Determines whether the given {@link Region} is a client, non-local {@link Region}.
   *
   * @param region {@link Region} to evaluate.
   * @return a boolean indicating whether the given {@link Region} is a client, non-local {@link Region}.
   * @see Region
   */
  boolean isNonLocalClientRegion(@Nullable Region<?, ?> region) {
    return GemFireUtils.isNonLocalClientRegion(region);
  }

  /**
   * Determines whether the given client {@link Region Region's} configured {@link Pool} has subscription enabled.
   *
   * @param region {@link Region} to evaluate.
   * @return a boolean value indicating whether the client {@link Region Region's} configured {@link Pool}
   * has subscription enabled.
   * @see Pool#getSubscriptionEnabled()
   * @see Region
   */
  boolean isRegionPoolSubscriptionEnabled(@Nullable Region<?, ?> region) {

    return Boolean.TRUE.equals(Optional.ofNullable(region)
        .map(Region::getAttributes)
        .map(RegionAttributes::getPoolName)
        .map(this::resolvePool)
        .map(Pool::getSubscriptionEnabled)
        .orElse(DEFAULT_CLIENT_SUBSCRIPTIONS_ENABLED));
  }

  /**
   * Determines whether the interest registration for the given {@link Region} is allowed.
   *
   * @param region {@link Region} to evaluate.
   * @return a boolean value indicating whether interest registration for the given {@link Region} is allowed.
   * @see Region#registerInterest(Object)
   * @see Region
   * @see #isNonLocalClientRegion(Region)
   * @see #isRegionPoolSubscriptionEnabled(Region)
   */
  boolean isRegionRegisterInterestAllowed(@Nullable Region<?, ?> region) {
    return isNonLocalClientRegion(region) && isRegionPoolSubscriptionEnabled(region);
  }

  /**
   * Resolves the {@link Pool} with the given {@link String name} from the {@link PoolManager}.
   *
   * @param name {@link String} containing the name of the {@link Pool} to resolve.
   * @return the resolved {@link Pool} for the given {@link String name}.
   * @see PoolManager#find(String)
   * @see Pool
   */
  protected @Nullable Pool resolvePool(String name) {
    return PoolManager.find(name);
  }

  /**
   * Constructs a new instance of {@link Logger} using Apache Commons {@link LoggerFactory}.
   *
   * @return a new instance of {@link Logger} constructed from Apache commons-logging {@link LoggerFactory}.
   * @see org.apache.commons.logging.LogFactory#getLog(Class)
   * @see org.apache.commons.logging.Log
   */
  private Logger newLogger() {
    return LoggerFactory.getLogger(getClass());
  }

  /**
   * Constructs a new instance of {@link SessionEventHandlerCacheListenerAdapter}.
   *
   * @return a new instance of {@link SessionEventHandlerCacheListenerAdapter}.
   * @see SessionEventHandlerCacheListenerAdapter
   */
  protected SessionEventHandlerCacheListenerAdapter newSessionEventHandler(BoundedRingHashSet ringHashSet) {
    return new SessionEventHandlerCacheListenerAdapter(this, ringHashSet);
  }

  /**
   * Constructs a new instance of {@link SessionEventHandlerCacheListenerAdapter}.
   *
   * @return a new instance of {@link SessionEventHandlerCacheListenerAdapter}.
   * @see SessionEventHandlerCacheListenerAdapter
   */
  protected SessionEventHandlerCacheWriterAdapter newSessionEventHandlerCacheWriterAdapter(BoundedRingHashSet ringHashSet) {
    return new SessionEventHandlerCacheWriterAdapter(this, ringHashSet);
  }

  /**
   * Sets the configured {@link ApplicationEventPublisher} used to publish {@link Session}
   * {@link AbstractSessionEvent events} corresponding to Apache Geode/Pivotal GemFire cache events.
   *
   * @param applicationEventPublisher {@link ApplicationEventPublisher} used to publish {@link Session}-based events;
   *                                  must not be {@literal null}.
   * @throws IllegalArgumentException if {@link ApplicationEventPublisher} is {@literal null}.
   * @see ApplicationEventPublisher
   */
  @Override
  public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {

    Assert.notNull(applicationEventPublisher, "ApplicationEventPublisher is required");

    this.applicationEventPublisher = applicationEventPublisher;
  }

  /**
   * Returns a reference to the configured {@link ApplicationEventPublisher} used to publish {@link Session}
   * {@link AbstractSessionEvent events} corresponding to Apache Geode/Pivotal GemFire cache events.
   *
   * @return the configured {@link ApplicationEventPublisher} used to publish {@link Session}
   * {@link AbstractSessionEvent events}.
   * @see ApplicationEventPublisher
   */
  protected @NonNull ApplicationEventPublisher getApplicationEventPublisher() {
    return this.applicationEventPublisher;
  }

  /**
   * Configures the {@link IsDirtyPredicate} strategy interface used to determine whether the users' application
   * domain objects are dirty or not.
   *
   * @param dirtyPredicate {@link IsDirtyPredicate} strategy interface implementation used to determine whether
   *                       the users' application domain objects are dirty or not.
   * @see IsDirtyPredicate
   */
  public void setIsDirtyPredicate(IsDirtyPredicate dirtyPredicate) {
    this.dirtyPredicate = dirtyPredicate;
  }

  /**
   * Returns the configured {@link IsDirtyPredicate} strategy interface implementation used to determine whether
   * the users' application domain objects are dirty or not.
   * <p>
   * Defaults to {@link GemFireHttpSessionConfiguration#DEFAULT_IS_DIRTY_PREDICATE}.
   *
   * @return the configured {@link IsDirtyPredicate} strategy interface used to determine whether
   * the users' application domain objects are dirty or not.
   * @see IsDirtyPredicate
   */
  public IsDirtyPredicate getIsDirtyPredicate() {

    return this.dirtyPredicate != null
        ? this.dirtyPredicate
        : DEFAULT_IS_DIRTY_PREDICATE;
  }

  /**
   * Return a reference to the {@link Logger} used to log messages.
   *
   * @return a reference to the {@link Logger} used to log messages.
   * @see org.apache.commons.logging.Log
   */
  protected Logger getLogger() {
    return this.logger;
  }

  /**
   * Sets the {@link Duration maximum interval} in which a {@link Session} can remain inactive
   * before the {@link Session} is considered expired.
   *
   * @param maxInactiveInterval {@link Duration} specifying the maximum interval that a {@link Session}
   *                            can remain inactive before the {@link Session} is considered expired.
   * @see Duration
   */
  public void setMaxInactiveInterval(Duration maxInactiveInterval) {
    this.maxInactiveInterval = maxInactiveInterval;
  }

  /**
   * Returns the {@link Duration maximum interval} in which a {@link Session} can remain inactive
   * before the {@link Session} is considered expired.
   *
   * @return a {@link Duration} specifying the maximum interval that a {@link Session} can remain inactive
   * before the {@link Session} is considered expired.
   * @see Duration
   */
  public Duration getMaxInactiveInterval() {
    return this.maxInactiveInterval;
  }

  /**
   * Sets the maximum interval in seconds in which a {@link Session} can remain inactive
   * before the {@link Session} is considered expired.
   *
   * @param maxInactiveIntervalInSeconds an integer value specifying the maximum interval in seconds
   *                                     that a {@link Session} can remain inactive before the {@link Session }is considered expired.
   * @see #setMaxInactiveInterval(Duration)
   */
  public void setMaxInactiveIntervalInSeconds(int maxInactiveIntervalInSeconds) {
    setMaxInactiveInterval(Duration.ofSeconds(maxInactiveIntervalInSeconds));
  }

  /**
   * Returns the maximum interval in seconds in which a {@link Session} can remain inactive
   * before the {@link Session} is considered expired.
   *
   * @return an integer value specifying the maximum interval in seconds that a {@link Session} can remain inactive
   * before the {@link Session} is considered expired.
   * @see #getMaxInactiveInterval()
   */
  public int getMaxInactiveIntervalInSeconds() {

    return Optional.ofNullable(getMaxInactiveInterval())
        .map(Duration::getSeconds)
        .map(Long::intValue)
        .orElse(0);
  }

  protected Optional<SessionEventHandlerCacheListenerAdapter> getSessionEventHandler() {
    return Optional.ofNullable(this.sessionEventHandler);
  }

  /**
   * Returns a reference to the configured Apache Geode / Pivotal GemFire cache {@link Region} used to
   * store and manage (HTTP) {@link Session} data.
   *
   * @return a reference to the configured {@link Session Sessions} {@link Region}.
   * @see Session
   * @see Region
   */
  protected @NonNull Region<Object, Session> getSessionsRegion() {
    return this.sessions;
  }

  /**
   * Returns the {@link String fully-qualified name} of the cache {@link Region} used to store
   * and manage {@link Session} state.
   *
   * @return a {@link String} containing the fully qualified name of the cache {@link Region}
   * used to store and manage {@link Session} data.
   * @see #getSessionsRegion()
   */
  protected String getSessionsRegionName() {
    return getSessionsRegion().getFullPath();
  }

  /**
   * Returns a reference to the {@link GemfireOperations template} used to perform data access operations
   * and other interactions on the cache {@link Region} storing and managing {@link Session} state
   * and backing this {@link SessionRepository}.
   *
   * @return a reference to the {@link GemfireOperations template} used to interact the {@link Region}
   * storing and managing {@link Session} state.
   * @see GemfireOperations
   */
  public @NonNull GemfireOperations getSessionsTemplate() {
    return this.template;
  }

  /**
   * @deprecated use {@link #getSessionsTemplate()}.
   */
  @Deprecated
  public @NonNull GemfireOperations getTemplate() {
    return getSessionsTemplate();
  }

  /**
   * Sets a condition indicating whether the DataSerialization framework has been configured.
   *
   * @param useDataSerialization boolean indicating whether the DataSerialization framework has been configured.
   */
  public void setUseDataSerialization(boolean useDataSerialization) {
    usingDataSerialization.set(useDataSerialization);
  }

  /**
   * Determines whether the DataSerialization framework has been configured.
   *
   * @return a boolean indicating whether the DataSerialization framework has been configured.
   * @see #resolveSystemUsingDataSerialization()
   */
  protected static boolean isUsingDataSerialization() {
    return usingDataSerialization.get() || resolveSystemUsingDataSerialization();
  }

  private static boolean resolveSystemUsingDataSerialization() {

    String configuredSessionSerializerBeanName =
        System.getProperty(GemFireHttpSessionConfiguration.SPRING_SESSION_DATA_GEMFIRE_SESSION_SERIALIZER_BEAN_NAME_PROPERTY,
            GemFireHttpSessionConfiguration.DEFAULT_SESSION_SERIALIZER_BEAN_NAME);

    boolean systemUsingDataSerialization = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
        .equals(configuredSessionSerializerBeanName);

    usingDataSerialization.set(systemUsingDataSerialization);

    return systemUsingDataSerialization;
  }

  /**
   * Commits the given {@link Session}.
   *
   * @param session {@link Session} to commit, iff the {@link Session} is {@literal committable}.
   * @return the given {@link Session}
   * @see GemFireSession#commit()
   */
  protected @Nullable Session commit(@Nullable Session session) {

    return Optional.ofNullable(session)
        .filter(GemFireSession.class::isInstance)
        .map(GemFireSession.class::cast)
        .<Session>map(gemfireSession -> {
          gemfireSession.commit();
          return gemfireSession;
        })
        .orElse(session);
  }

  protected @Nullable Session configure(@Nullable Session session) {

    return Optional.ofNullable(session)
        .filter(GemFireSession.class::isInstance)
        .map(GemFireSession.class::cast)
        .map(it -> it.configureWith(getMaxInactiveInterval()))
        .<Session>map(it -> it.configureWith(getIsDirtyPredicate()))
        .orElse(session);
  }

  /**
   * Deletes the given {@link Session} from Apache Geode / Pivotal GemFire.
   *
   * @param session {@link Session} to delete.
   * @return {@literal null}.
   * @see Session#getId()
   * @see Session
   * @see #deleteById(String)
   */
  protected @Nullable Session delete(@NonNull Session session) {

    deleteById(session.getId());

    return null;
  }

  /**
   * Handles the deletion of the given {@link Session}.
   *
   * @param sessionId {@link String} containing the {@link Session#getId()} of the given {@link Session}.
   * @param session   deleted {@link Session}.
   * @see SessionEventHandlerCacheListenerAdapter#afterDelete(String, Session)
   * @see Session
   */
  protected void handleDeleted(String sessionId, Session session) {
    Optional.ofNullable(session).ifPresentOrElse(session1 -> {
      if (session.isExpired()) {
        getSessionEventHandler()
            .ifPresent(it -> it.afterExpired(sessionId, session));
      } else {
        getSessionEventHandler().ifPresent(it -> it.afterDelete(sessionId, session));
      }
    }, () -> getSessionEventHandler().ifPresent(it -> it.afterDelete(sessionId, session)));

  }

  /**
   * Publishes the specified {@link ApplicationEvent} to the Spring container thereby notifying other (potentially)
   * interested application components/beans.
   *
   * @param event {@link ApplicationEvent} to publish.
   * @see ApplicationEventPublisher#publishEvent(ApplicationEvent)
   * @see ApplicationEvent
   */
  protected void publishEvent(ApplicationEvent event) {

    try {
      getApplicationEventPublisher().publishEvent(event);
    } catch (Throwable cause) {
      getLogger().error(String.format("Error occurred while publishing event [%s]", event), cause);
    }
  }

  /**
   * Updates the {@link Session#setLastAccessedTime(Instant)} property of the {@link Session}
   * to the {@link Instant#now() current time}.
   *
   * @param session {@link Session} to touch.
   * @return the {@link Session}.
   * @see Session#setLastAccessedTime(Instant)
   * @see Session
   * @see Instant#now()
   */
  protected @NonNull Session touch(@NonNull Session session) {

    session.setLastAccessedTime(Instant.now());

    return session;
  }

  @SuppressWarnings("unused")
  public static class DeltaCapableGemFireSession
      extends GemFireSession<DeltaCapableGemFireSessionAttributes> implements Delta {

    public DeltaCapableGemFireSession() {
    }

    public DeltaCapableGemFireSession(String id) {
      super(id);
    }

    public DeltaCapableGemFireSession(Session session) {
      super(session);
    }

    @Override
    protected DeltaCapableGemFireSessionAttributes newSessionAttributes(Object lock) {

      return new DeltaCapableGemFireSessionAttributes(lock)
          .configureWith(getIsDirtyPredicate());
    }

    public synchronized void toDelta(DataOutput out) throws IOException {

      out.writeUTF(getId());
      out.writeLong(getLastAccessedTime().toEpochMilli());
      out.writeLong(getMaxInactiveInterval().getSeconds());
      getAttributes().toDelta(out);
    }

    public synchronized void fromDelta(DataInput in) throws IOException {

      setId(in.readUTF());
      setLastAccessedTime(Instant.ofEpochMilli(in.readLong()));
      setMaxInactiveInterval(Duration.ofSeconds(in.readLong()));
      getAttributes().fromDelta(in);
    }
  }

  /**
   * {@link GemFireSession} is a Abstract Data Type (ADT) for a Spring {@link Session} that stores and manages
   * {@link Session} state in Apache Geode or Pivotal GemFire.
   *
   * @see Comparable
   * @see Session
   * @see GemFireSessionAttributes
   */
  public static class GemFireSession<T extends GemFireSessionAttributes> implements Comparable<Session>, Session {

    protected static final String GEMFIRE_SESSION_TO_STRING =
        "{ @type = %1$s, id = %2$s, creationTime = %3$s, lastAccessedTime = %4$s, maxInactiveInterval = %5$s, principalName = %6$s }";

    protected static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

    /**
     * Factory method used to construct a new, default instance of {@link GemFireSession}.
     *
     * @param <T> {@link Class Sub-type} of {@link GemFireSessionAttributes}.
     * @return a new {@link GemFireSession}.
     * @see #isUsingDataSerialization()
     */
    @SuppressWarnings("unchecked")
    public static <T extends GemFireSessionAttributes> GemFireSession<T> create() {

      return isUsingDataSerialization()
          ? (GemFireSession<T>) new DeltaCapableGemFireSession()
          : new GemFireSession<>();
    }

    /**
     * Copy (i.e. clone) the given {@link Session}.
     *
     * @param session {@link Session} to copy/clone.
     * @return a new instance of {@link GemFireSession} copied from the given {@link Session}.
     * @see Session
     * @see #isUsingDataSerialization()
     */
    @SuppressWarnings("rawtypes")
    public static GemFireSession copy(@NonNull Session session) {

      return isUsingDataSerialization()
          ? new DeltaCapableGemFireSession(session)
          : new GemFireSession(session);
    }

    /**
     * Returns the given {@link Session} if the {@link Session} is a {@link GemFireSession}
     * or return a copy of the given {@link Session} as a {@link GemFireSession}.
     *
     * @param session {@link Session} to evaluate and possibly copy.
     * @return the given {@link Session} if the {@link Session} is a {@link GemFireSession}
     * or return a copy of the given {@link Session} as a {@link GemFireSession}.
     * @see #copy(Session)
     */
    @SuppressWarnings("rawtypes")
    public static GemFireSession from(@NonNull Session session) {
      return session instanceof GemFireSession ? (GemFireSession) session : copy(session);
    }

    private transient boolean delta = true;

    private Duration maxInactiveInterval;

    private final Instant creationTime;

    private Instant lastAccessedTime;

    private transient IsDirtyPredicate dirtyPredicate = DEFAULT_IS_DIRTY_PREDICATE;

    private transient final SpelExpressionParser parser = new SpelExpressionParser();

    private String id;

    private transient final T sessionAttributes = newSessionAttributes(this);

    /**
     * Constructs a new, default instance of {@link GemFireSession} initialized with
     * a generated {@link Session#getId() Session Identifier}.
     *
     * @see #GemFireSession(String)
     * @see #generateSessionId()
     */
    protected GemFireSession() {
      this(generateSessionId());
    }

    /**
     * Constructs a new instance of {@link GemFireSession} initialized with
     * the given {@link Session#getId() Session Identifier}.
     * <p>
     * Additionally, the {@link #creationTime} is set to {@link Instant#now()}, {@link #lastAccessedTime}
     * is set to {@link #creationTime} and the {@link #maxInactiveInterval} is set to {@link Duration#ZERO}.
     *
     * @param id {@link String} containing the unique identifier for this {@link Session}.
     * @see #validateSessionId(String)
     */
    protected GemFireSession(String id) {

      this.id = validateSessionId(id);
      this.creationTime = Instant.now();
      this.lastAccessedTime = this.creationTime;
      this.maxInactiveInterval = Duration.ZERO;
    }

    /**
     * Constructs a new instance of {@link GemFireSession} copied from the given {@link Session}.
     *
     * @param session {@link Session} to copy.
     * @throws IllegalArgumentException if {@link Session} is {@literal null}.
     * @see Session
     */
    protected GemFireSession(Session session) {

      Assert.notNull(session, "Session is required");

      this.id = session.getId();
      this.creationTime = session.getCreationTime();
      this.lastAccessedTime = session.getLastAccessedTime();
      this.maxInactiveInterval = session.getMaxInactiveInterval();
      this.sessionAttributes.from(session);
    }

    /**
     * Constructs a new {@link GemFireSessionAttributes} object to store and manage Session attributes.
     *
     * @param lock {@link Object} used as the mutex for concurrent access and Thread-safety.
     * @return the new {@link GemFireSessionAttributes}.
     * @see GemFireSessionAttributes
     * @see #getIsDirtyPredicate()
     */
    protected T newSessionAttributes(Object lock) {

      return new GemFireSessionAttributes(lock)
          .configureWith(getIsDirtyPredicate());
    }

    /**
     * Change the {@link String identifier} of this {@link Session}.
     *
     * @return the new {@link String identifier} of of this {@link Session}.
     * @see #generateSessionId()
     * @see #triggerDelta()
     * @see #getId()
     */
    @Override
    public synchronized String changeSessionId() {

      this.id = generateSessionId();

      triggerDelta();

      return getId();
    }

    /**
     * Randomly generates a {@link String unique identifier} (ID) from {@link UUID} to be used as
     * the {@link Session#getId() ID} for this {@link Session}.
     *
     * @return a new {@link String unique identifier (ID)}.
     * @see UUID#randomUUID()
     */
    private static String generateSessionId() {
      return UUID.randomUUID().toString();
    }

    /**
     * Validates the given {@link Session} {@link String identifier} (ID) is set and valid.
     *
     * @param id {@link String} containing the {@link Session} identifier.
     * @return the given {@link String ID}.
     * @throws IllegalArgumentException if {@link String} contains no value.
     */
    private static String validateSessionId(String id) {

      Assert.hasText(id, "ID is required");

      return id;
    }

    protected synchronized void commit() {
      this.delta = false;
      getAttributes().commit();
    }

    /**
     * Determines whether this {@link GemFireSession} has any changes (i.e. a delta).
     * <p>
     * Changes exist if this {@link GemFireSession GemFireSession's} {@link #getId() ID},
     * {@link #getLastAccessedTime() last accessed time}, {@link #getMaxInactiveInterval() max inactive interval}
     * or any of these {@link #getAttributeNames() attributes} have changed.
     *
     * @return a boolean value indicating whether this {@link GemFireSession} has any changes.
     * @see GemFireSessionAttributes#hasDelta()
     * @see #getAttributes()
     */
    public synchronized boolean hasDelta() {
      return this.delta || getAttributes().hasDelta();
    }

    protected synchronized void triggerDelta() {
      triggerDelta(true);
    }

    protected synchronized void triggerDelta(boolean delta) {
      this.delta |= delta;
    }

    synchronized void setId(String id) {
      this.id = validateSessionId(id);
    }

    public synchronized String getId() {
      return this.id;
    }

    public void setAttribute(String attributeName, Object attributeValue) {
      getAttributes().setAttribute(attributeName, attributeValue);
    }

    public void removeAttribute(String attributeName) {
      getAttributes().removeAttribute(attributeName);
    }

    public <T> T getAttribute(String attributeName) {
      return getAttributes().getAttribute(attributeName);
    }

    public Set<String> getAttributeNames() {
      return getAttributes().getAttributeNames();
    }

    public T getAttributes() {
      return this.sessionAttributes;
    }

    public synchronized Instant getCreationTime() {
      return this.creationTime;
    }

    public synchronized boolean isExpired() {

      Instant lastAccessedTime = getLastAccessedTime();

      Duration maxInactiveInterval = getMaxInactiveInterval();

      return isExpirationEnabled(maxInactiveInterval)
          && Instant.now().minus(maxInactiveInterval).isAfter(lastAccessedTime);
    }

    private boolean isExpirationEnabled(Duration duration) {
      return duration != null && duration.toNanos() > 0;
    }

    protected synchronized void setIsDirtyPredicate(IsDirtyPredicate dirtyPredicate) {

      this.dirtyPredicate = dirtyPredicate;
      getAttributes().configureWith(dirtyPredicate);
    }

    protected synchronized IsDirtyPredicate getIsDirtyPredicate() {

      return this.dirtyPredicate != null
          ? this.dirtyPredicate
          : DEFAULT_IS_DIRTY_PREDICATE;
    }

    private boolean isLastAccessedTimeValid(Instant lastAccessedTime) {
      return lastAccessedTime != null;
    }

    public synchronized void setLastAccessedTime(Instant lastAccessedTime) {

      if (isLastAccessedTimeValid(lastAccessedTime)) {

        triggerDelta(!ObjectUtils.nullSafeEquals(this.lastAccessedTime, lastAccessedTime));

        this.lastAccessedTime = lastAccessedTime;
      }
    }

    public synchronized Instant getLastAccessedTime() {
      return this.lastAccessedTime;
    }

    public synchronized void setMaxInactiveInterval(Duration maxInactiveInterval) {

      triggerDelta(!ObjectUtils.nullSafeEquals(this.maxInactiveInterval, maxInactiveInterval));

      this.maxInactiveInterval = maxInactiveInterval;
    }

    public synchronized Duration getMaxInactiveInterval() {

      return this.maxInactiveInterval != null
          ? this.maxInactiveInterval
          : Duration.ZERO;
    }

    public synchronized void setPrincipalName(String principalName) {
      setAttribute(PRINCIPAL_NAME_INDEX_NAME, principalName);
    }

    public synchronized String getPrincipalName() {

      String principalName = getAttribute(PRINCIPAL_NAME_INDEX_NAME);

      if (principalName == null) {

        Object authentication = getAttribute(SPRING_SECURITY_CONTEXT);

        if (authentication != null) {

          Expression expression = this.parser.parseExpression("authentication?.name");

          principalName = expression.getValue(authentication, String.class);
        }
      }

      return principalName;
    }

    /**
     * Builder method to configure the {@link Duration max inactive interval} before this {@link GemFireSession}
     * will expire.
     *
     * @param maxInactiveInterval {@link Duration} specifying the maximum time this {@link GemFireSession}
     *                            can remain inactive before expiration.
     * @return this {@link GemFireSession}.
     * @see #setMaxInactiveInterval(Duration)
     * @see Duration
     */
    public GemFireSession<T> configureWith(Duration maxInactiveInterval) {
      setMaxInactiveInterval(maxInactiveInterval);
      return this;
    }

    /**
     * Builder method to configure the {@link IsDirtyPredicate} strategy interface implementation to determine
     * whether users' {@link Object application domain objects} stored in this {@link GemFireSession} are dirty.
     *
     * @param dirtyPredicate {@link IsDirtyPredicate} strategy interface implementation that determines whether
     *                       the users' {@link Object application domain objects} stored in this {@link GemFireSession} are dirty.
     * @return this {@link GemFireSession}.
     * @see IsDirtyPredicate
     * @see #setIsDirtyPredicate(IsDirtyPredicate)
     */
    public GemFireSession<T> configureWith(IsDirtyPredicate dirtyPredicate) {
      setIsDirtyPredicate(dirtyPredicate);
      return this;
    }

    @Override
    public int compareTo(Session session) {
      return getCreationTime().compareTo(session.getCreationTime());
    }

    @Override
    public boolean equals(final Object obj) {

      if (this == obj) {
        return true;
      }

      if (!(obj instanceof Session)) {
        return false;
      }

      Session that = (Session) obj;

      return this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {

      int hashValue = 17;

      hashValue = 37 * hashValue + getId().hashCode();

      return hashValue;
    }

    @Override
    public synchronized String toString() {

      return String.format(GEMFIRE_SESSION_TO_STRING, getClass().getName(), getId(), getCreationTime(),
          getLastAccessedTime(), getMaxInactiveInterval(), getPrincipalName());
    }
  }

  public static class DeltaCapableGemFireSessionAttributes extends GemFireSessionAttributes implements Delta {

    private transient final Set<String> sessionAttributeDeltas = new HashSet<>();

    public DeltaCapableGemFireSessionAttributes() {
    }

    public DeltaCapableGemFireSessionAttributes(Object lock) {
      super(lock);
    }

    Set<String> getSessionAttributeDeltas() {

      synchronized (getLock()) {
        return this.sessionAttributeDeltas;
      }
    }

    @Override
    protected BiFunction<String, Object, Boolean> sessionAttributesChangeInterceptor() {

      return (attributeName, attributeValue) -> {
        getSessionAttributeDeltas().add(attributeName);
        return true;
      };
    }

    public void toDelta(DataOutput out) throws IOException {

      synchronized (getLock()) {

        Set<String> sessionAttributeDeltas = getSessionAttributeDeltas();

        out.writeInt(sessionAttributeDeltas.size());

        for (String attributeName : sessionAttributeDeltas) {
          out.writeUTF(attributeName);
          writeObject(getAttribute(attributeName), out);
        }
      }
    }

    protected void writeObject(Object value, DataOutput out) throws IOException {
      DataSerializer.writeObject(value, out);
    }

    @Override
    public boolean hasDelta() {

      synchronized (getLock()) {
        return !getSessionAttributeDeltas().isEmpty();
      }
    }

    public void fromDelta(DataInput in) throws InvalidDeltaException, IOException {

      synchronized (getLock()) {
        try {

          int count = in.readInt();

          Map<String, Object> deltas = new HashMap<>(count);

          while (count-- > 0) {
            deltas.put(in.readUTF(), readObject(in));
          }

          Set<String> sessionAttributeDeltas = getSessionAttributeDeltas();

          deltas.forEach((key, value) -> {
            setAttribute(key, value);
            sessionAttributeDeltas.remove(key);
          });
        } catch (ClassNotFoundException cause) {
          throw new InvalidDeltaException("Class type in data not found", cause);
        }
      }
    }

    protected <T> T readObject(DataInput in) throws ClassNotFoundException, IOException {
      return DataSerializer.readObject(in);
    }

    @Override
    protected void commit() {

      synchronized (getLock()) {
        getSessionAttributeDeltas().clear();
        super.commit();
      }
    }
  }

  /**
   * The {@link GemFireSessionAttributes} class is a container for Session attributes implementing
   * both the {@link DataSerializable} and {@link Delta} Pivotal GemFire interfaces for efficient
   * storage and distribution (replication) in GemFire. Additionally, GemFireSessionAttributes
   * extends {@link AbstractMap} providing {@link Map}-like behavior since attributes of a Session
   * are effectively a name to value mapping.
   *
   * @see AbstractMap
   * @see DataSerializable
   * @see DataSerializer
   * @see Delta
   */
  @SuppressWarnings("serial")
  public static class GemFireSessionAttributes extends AbstractMap<String, Object> {

    public static GemFireSessionAttributes create() {
      return new GemFireSessionAttributes();
    }

    public static GemFireSessionAttributes create(Object lock) {
      return new GemFireSessionAttributes(lock);
    }

    private transient boolean delta = false;

    private transient IsDirtyPredicate dirtyPredicate = DEFAULT_IS_DIRTY_PREDICATE;

    private transient final Map<String, Object> sessionAttributes = new HashMap<>();

    private transient final Object lock;

    /**
     * Constructs a new instance of {@link GemFireSessionAttributes}.
     */
    protected GemFireSessionAttributes() {
      this.lock = this;
    }

    /**
     * Constructs a new instance of {@link GemFireSessionAttributes} initialized with the given {@link Object lock}
     * to use to guard against concurrent access by multiple {@link Thread Threads}.
     *
     * @param lock {@link Object} used as the {@literal mutex} to guard the operations of this object
     *             from concurrent access by multiple {@link Thread Threads}.
     */
    protected GemFireSessionAttributes(@Nullable Object lock) {
      this.lock = lock != null ? lock : this;
    }

    /**
     * Returns a reference to the internal, {@link Session} attributes data structure.
     *
     * @return a reference to the internal, {@link Session} attributes data structure.
     * @see Map
     */
    Map<String, Object> getMap() {
      return this.sessionAttributes;
    }

    /**
     * Returns the {@link Object} used as the {@literal lock} guarding the methods of this object
     * from concurrent access by multiple {@link Thread Threads}.
     *
     * @return the {@link Object lock} guarding the methods of this object from concurrent access
     * by multiple {@link Thread Threads}.
     */
    public Object getLock() {
      return this.lock;
    }

    protected void setIsDirtyPredicate(IsDirtyPredicate dirtyPredicate) {

      synchronized (getLock()) {
        this.dirtyPredicate = dirtyPredicate;
      }
    }

    protected IsDirtyPredicate getIsDirtyPredicate() {

      synchronized (getLock()) {
        return this.dirtyPredicate != null
            ? this.dirtyPredicate
            : DEFAULT_IS_DIRTY_PREDICATE;
      }
    }

    public Object setAttribute(String attributeName, Object attributeValue) {

      synchronized (getLock()) {
        return attributeValue != null
            ? doSetAttribute(attributeName, attributeValue)
            : removeAttribute(attributeName);
      }
    }

    private Object doSetAttribute(String attributeName, Object attributeValue) {

      Map<String, Object> sessionAttributes = getMap();

      Object previousAttributeValue = sessionAttributes.put(attributeName, attributeValue);

      this.delta |= getIsDirtyPredicate().isDirty(previousAttributeValue, attributeValue)
          && sessionAttributesChangeInterceptor().apply(attributeName, attributeValue);

      return previousAttributeValue;
    }

    public Object removeAttribute(String attributeName) {

      synchronized (getLock()) {

        Map<String, Object> sessionAttributes = getMap();

        this.delta |= sessionAttributes.containsKey(attributeName)
            && sessionAttributesChangeInterceptor().apply(attributeName, null);

        return sessionAttributes.remove(attributeName);
      }
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String attributeName) {

      synchronized (getLock()) {
        return (T) getMap().get(attributeName);
      }
    }

    public Set<String> getAttributeNames() {

      synchronized (getLock()) {
        return Collections.unmodifiableSet(getMap().keySet());
      }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {

      synchronized (getLock()) {

        return new AbstractSet<Entry<String, Object>>() {

          @Override
          public Iterator<Entry<String, Object>> iterator() {
            return Collections.unmodifiableMap(GemFireSessionAttributes.this.getMap())
                .entrySet().iterator();
          }

          @Override
          public int size() {
            return GemFireSessionAttributes.this.getMap().size();
          }
        };
      }
    }

    protected BiFunction<String, Object, Boolean> sessionAttributesChangeInterceptor() {
      return (attributeName, attributeValue) -> true;
    }

    protected void commit() {

      synchronized (getLock()) {
        this.delta = false;
      }
    }

    @SuppressWarnings("unchecked")
    public <T extends GemFireSessionAttributes> T configureWith(IsDirtyPredicate dirtyPredicate) {
      setIsDirtyPredicate(dirtyPredicate);
      return (T) this;
    }

    public void from(Session session) {

      synchronized (getLock()) {
        session.getAttributeNames().forEach(attributeName ->
            setAttribute(attributeName, session.getAttribute(attributeName)));
      }
    }

    public void from(Map<String, Object> map) {

      synchronized (getLock()) {
        map.forEach(this::setAttribute);
      }
    }

    public void from(GemFireSessionAttributes sessionAttributes) {

      synchronized (getLock()) {
        sessionAttributes.getAttributeNames().forEach(attributeName ->
            setAttribute(attributeName, sessionAttributes.getAttribute(attributeName)));
      }
    }

    public boolean hasDelta() {

      synchronized (getLock()) {
        return this.delta;
      }

    }

    @Override
    public String toString() {

      synchronized (getLock()) {
        return getMap().toString();
      }
    }
  }
}
