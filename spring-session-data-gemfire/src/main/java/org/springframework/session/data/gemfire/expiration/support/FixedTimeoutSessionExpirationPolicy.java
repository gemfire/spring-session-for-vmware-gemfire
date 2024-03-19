/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.support;

import java.time.Duration;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy;
import org.springframework.util.Assert;

/**
 * An implementation of the {@link SessionExpirationPolicy} interface that specifies an expiration policy based on
 * a {@link Duration fixed duration of time}.
 *
 * In other words, the {@link Session} will timeout after a {@link Duration fixed duration of time} even if
 * the {@link Session} is still active (i.e. not idle).
 *
 * @author John Blum
 * @see Duration
 * @see Session
 * @see SessionExpirationPolicy
 * @see IdleTimeoutSessionExpirationPolicy
 * @since 2.1.0
 */
@SuppressWarnings("unused")
public class FixedTimeoutSessionExpirationPolicy extends IdleTimeoutSessionExpirationPolicy {

	private final Duration fixedTimeout;

	/**
	 * Constructs a new {@link FixedTimeoutSessionExpirationPolicy} initialized with
	 * the given {@link Duration fixed, expiration timeout}.
	 *
	 * @param fixedTimeout {@link Duration fixed length of time} until the {@link Session} will expire.
	 * @throws IllegalArgumentException if the {@link Duration fixed timeout} is {@literal null}.
	 * @see Duration
	 */
	public FixedTimeoutSessionExpirationPolicy(@NonNull Duration fixedTimeout) {

		super(null);

		Assert.notNull(fixedTimeout, "Fixed expiration timeout is required");

		this.fixedTimeout = fixedTimeout;
	}

	/**
	 * Return the configured {@link Duration fixed expiration timeout}.
	 *
	 * @return the configured {@link Duration fixed expiration timeout}.
	 * @see Duration
	 */
	protected Duration getFixedTimeout() {
		return this.fixedTimeout;
	}

	@Override
	public Optional<Duration> determineExpirationTimeout(@NonNull Session session) {

		Optional<Duration> idleTimeout = super.determineExpirationTimeout(session);

		Duration fixedExpirationTimeout = getFixedTimeout().minus(computeTimeSinceCreation(session));

		return idleTimeout.filter(it -> it.compareTo(fixedExpirationTimeout) <= 0).isPresent()
			? Optional.empty() : Optional.of(fixedExpirationTimeout);
	}

	private Duration computeTimeSinceCreation(@NonNull Session session) {

		long timeSinceCreation = Math.max(System.currentTimeMillis() - session.getCreationTime().toEpochMilli(), 0L);

		return Duration.ofMillis(timeSinceCreation);
	}
}
