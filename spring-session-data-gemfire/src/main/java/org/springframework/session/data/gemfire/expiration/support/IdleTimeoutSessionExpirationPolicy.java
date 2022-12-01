/*
 * Copyright (c) VMware, Inc. 2022. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.support;

import java.time.Duration;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy;
import org.springframework.session.data.gemfire.expiration.config.SessionExpirationTimeoutAware;

/**
 * An implementation of the {@link SessionExpirationPolicy} interface that specifies an expiration policy for
 * {@link Session Sessions} that have been idle, or inactive for a predefined {@link Duration duration of time}.
 *
 * @author John Blum
 * @see Duration
 * @see Optional
 * @see Session
 * @see SessionExpirationPolicy
 * @see SessionExpirationTimeoutAware
 * @since 2.1.0
 */
@SuppressWarnings("unused")
public class IdleTimeoutSessionExpirationPolicy implements SessionExpirationPolicy, SessionExpirationTimeoutAware {

	protected static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(30L);

	private Duration idleTimeout;

	/**
	 * Constructs a new {@link IdleTimeoutSessionExpirationPolicy} initialized with
	 * the {@link IdleTimeoutSessionExpirationPolicy#DEFAULT_IDLE_TIMEOUT}.
	 *
	 * @see IdleTimeoutSessionExpirationPolicy
	 * 	#DEFAULT_IDLE_TIMEOUT
	 */
	public IdleTimeoutSessionExpirationPolicy() {
		this(DEFAULT_IDLE_TIMEOUT);
	}

	/**
	 * Constructs a new {@link IdleTimeoutSessionExpirationPolicy} initialized with
	 * the given {@link Duration idle timeout}.
	 *
	 * @param idleTimeout {@link Duration length of time} until an idle, or inactive {@link Session} should expire;
	 * Maybe {@literal null} to suggest the {@link Session} should not expire.
	 * @see Duration
	 */
	public IdleTimeoutSessionExpirationPolicy(@Nullable Duration idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	/**
	 * Configures the expiration {@link Duration idle timeout}.
	 *
	 * @param idleTimeout {@link Duration length of time} until an idle, or inactive {@link Session} should expire;
	 * Maybe {@literal null} to suggest the {@link Session} should not expire.
	 * @see Duration
	 */
	@Override
	public void setExpirationTimeout(@Nullable Duration idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	/**
	 * Return an {@link Optional optionally} configured expiration {@link Duration idle timeout}.
	 *
	 * @return the {@link Optional optionally} configured expiration {@link Duration idle timeout}.
	 * @see Duration
	 * @see Optional
	 */
	protected Optional<Duration> getIdleTimeout() {
		return Optional.ofNullable(this.idleTimeout);
	}

	@Override @SuppressWarnings("all")
	public Optional<Duration> determineExpirationTimeout(@NonNull Session session) {

		return getIdleTimeout()
			.map(idleTimeout -> idleTimeout.minus(computeIdleTime(session)));
	}

	private Duration computeIdleTime(@NonNull Session session) {

		long idleTime = Math.max(System.currentTimeMillis() - session.getLastAccessedTime().toEpochMilli(), 0L);

		return Duration.ofMillis(idleTime);
	}
}
