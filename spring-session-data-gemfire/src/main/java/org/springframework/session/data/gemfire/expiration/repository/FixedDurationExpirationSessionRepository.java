/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import org.apache.shiro.util.Assert;

/**
 * The {@link FixedDurationExpirationSessionRepository} class is a {@link SessionRepository} implementation wrapping
 * an existing {@link SessionRepository}, data store specific, implementation in order to implement
 * a fixed {@link Duration} expiration policy on the {@link Session}.
 *
 * That is, the {@link Session} will always expire (or be considered "expired") after a fixed amount of time.  Even if
 * the user {@link Session} is still actively being accessed up to the last moment right before the {@link Session}
 * is about to expire, the {@link Session} will expire regardless.
 *
 * This may be useful in certain UCs where, for security reasons, the {@link Session} must expire no matter what.
 *
 * @author John Blum
 * @see Duration
 * @see Instant
 * @see Session
 * @see SessionRepository
 * @see <a href="https://github.com/spring-projects/spring-session/issues/922">Absolute Session Timeouts</a>
 * @since 2.1.0
 */
@SuppressWarnings("unused")
public class FixedDurationExpirationSessionRepository<S extends Session> implements SessionRepository<S> {

	private final SessionRepository<S> delegate;

	private final Duration expirationTimeout;

	/**
	 * Constructs a new instance of {@link FixedDurationExpirationSessionRepository} initialized with the given
	 * data store specific {@link SessionRepository}.
	 *
	 * @param sessionRepository {@link SessionRepository} delegate.
	 * @param expirationTimeout {@link Duration} specifying the length of time until the {@link Session} expires.
	 * @throws IllegalArgumentException if {@link SessionRepository} is {@literal null}.
	 * @see SessionRepository
	 * @see Duration
	 */
	public FixedDurationExpirationSessionRepository(@NonNull SessionRepository<S> sessionRepository,
			@Nullable Duration expirationTimeout) {

		Assert.notNull(sessionRepository, "SessionRepository is required");

		this.delegate = sessionRepository;
		this.expirationTimeout = expirationTimeout;
	}

	/**
	 * Returns a reference to the data store specific {@link SessionRepository}.
	 *
	 * @return a reference to the data store specific {@link SessionRepository}.
	 * @see SessionRepository
	 */
	@NonNull
	protected SessionRepository<S> getDelegate() {
		return this.delegate;
	}

	/**
	 * Return an {@link Optional} {@link Duration expiraiton timeout}.
	 *
	 * @return an {@link Optional} {@link Duration expiraiton timeout}.
	 * @see Duration
	 * @see Optional
	 */
	public Optional<Duration> getExpirationTimeout() {
		return Optional.ofNullable(this.expirationTimeout);
	}

	/**
	 * Creates a new instance of {@link Session}.
	 *
	 * The {@link Session} instance will be managed by Apache Geode or Pivotal GemFire.
	 *
	 * @return a new instance of {@link Session}.
	 * @see Session
	 */
	@Override
	public S createSession() {
		return getDelegate().createSession();
	}

	/**
	 * Finds a {@link Session} with the given {@link String ID}.
	 *
	 * This method will also perform a lazy expiration check to determine if the {@link Session} has already expired
	 * upon access, and if so, delete the {@link Session} with the given {@link String ID}.
	 *
	 * @param id {@link String} containing the ID identifying the {@link Session} to lookup.
	 * @return the {@link Session} with the given {@link String ID} or {@literal null} if no {@link Session}
	 * with {@link String ID} exists or the {@link Session} is expired.
	 * @see Session
	 * @see #handleExpired(Session, Duration)
	 * @see #getExpirationTimeout()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public S findById(String id) {

		return Optional.ofNullable(getDelegate().findById(id))
			.map(session -> getExpirationTimeout()
				.map(expirationDuration -> handleExpired(session, expirationDuration))
				.orElseGet(()-> getExpirationTimeout().isPresent() ? null : session)
			).orElse(null);
	}

	/**
	 * Handles the expiration event for the given {@link Session} if the {@link Session} has expired.
	 *
	 * @param session {@link Session} to evaluate for expiration.
	 * @param expirationDuration {@link Duration} indicating the length of time before an idle,
	 * unused or old {@link Session} expires.
	 * @return the given {@link Session} or {@literal null} if the {@link Session} has already expired.
	 * @see #isExpired(Session, Duration)
	 * @see #deleteById(String)
	 * @see Session
	 * @see Duration
	 */
	S handleExpired(S session, Duration expirationDuration) {

		if (isExpired(session, expirationDuration)) {
			deleteById(session.getId());
			session = null;
		}

		return session;
	}

	/**
	 * Determines whether the given {@link Session} has expired.
	 *
	 * This {@link SessionRepository} implements fixed duration expiration, which means the {@link Session} will expire
	 * after a fixed length of time (i.e. a fixed {@link Duration}).  Even if the user {@link Session} is active
	 * (i.e. not idle), the {@link Session} will still expire after the fixed {@link Duration} is exceeded.
	 *
	 * @param session {@link Session} to evaluate.
	 * @param expirationDuration {@link Duration} indicating the length of time before the {@link Session} expires.
	 * @return a boolean value indication whether the {@link Session} has expired.
	 * @see Session
	 * @see Duration
	 * @see Instant
	 */
	boolean isExpired(S session, Duration expirationDuration) {

		Instant sessionCreationTime = session.getCreationTime();
		Instant now = Instant.now();

		return now.minusMillis(expirationDuration.toMillis()).isAfter(sessionCreationTime);
	}

	/**
	 * Saves the given {@link Session} to the underlying data (persistent) store.
	 *
	 * @param session {@link Session} to save.
	 * @see Session
	 */
	@Override
	public void save(S session) {
		getDelegate().save(session);
	}

	/**
	 * Deletes the {@link Session} identified by the given {@link String ID}.
	 *
	 * @param id {@link String} containing the ID of the {@link Session} to delete.
	 * @see Session
	 */
	@Override
	public void deleteById(String id) {
		getDelegate().deleteById(id);
	}
}
