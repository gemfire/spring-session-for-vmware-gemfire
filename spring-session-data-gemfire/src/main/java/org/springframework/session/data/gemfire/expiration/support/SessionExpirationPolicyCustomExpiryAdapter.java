/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.support;

import java.time.Duration;
import java.util.Optional;

import org.apache.geode.cache.CustomExpiry;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.pdx.PdxInstance;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy;
import org.springframework.util.Assert;

/**
 * The {@link SessionExpirationPolicyCustomExpiryAdapter} class is an Apache Geode/Pivotal GemFire {@link CustomExpiry}
 * implementation wrapping and adapting an instance of the {@link SessionExpirationPolicy} strategy interface
 * to plugin to and affect Apache Geode/Pivotal GemFire's expiration behavior.
 *
 * @author John Blum
 * @see CustomExpiry
 * @see ExpirationAction
 * @see ExpirationAttributes
 * @see Region
 * @see PdxInstance
 * @see Session
 * @see SessionExpirationPolicy
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class SessionExpirationPolicyCustomExpiryAdapter implements CustomExpiry<String, Object> {

	protected static final SessionExpirationPolicy.ExpirationAction DEFAULT_EXPIRATION_ACTION =
		SessionExpirationPolicy.ExpirationAction.INVALIDATE;

	private final SessionExpirationPolicy sessionExpirationPolicy;

	/**
	 * Constructs a new {@link SessionExpirationPolicyCustomExpiryAdapter} initialized with
	 * the given, required {@link SessionExpirationPolicy}.
	 *
	 * @param sessionExpirationPolicy {@link SessionExpirationPolicy} used to determine the expiration policy
	 * for all {@link Session Sessions}.
	 * @throws IllegalArgumentException if {@link SessionExpirationPolicy} is {@literal null}.
	 * @see SessionExpirationPolicy
	 */
	public SessionExpirationPolicyCustomExpiryAdapter(@NonNull SessionExpirationPolicy sessionExpirationPolicy) {

		Assert.notNull(sessionExpirationPolicy, "SessionExpirationPolicy is required");

		this.sessionExpirationPolicy = sessionExpirationPolicy;
	}

	/**
	 * Returns the configured {@link SessionExpirationPolicy} defining the expiration policies
	 * for all managed {@link Session Sessions}.
	 *
	 * @return the configured {@link SessionExpirationPolicy}.
	 * @see SessionExpirationPolicy
	 */
	protected SessionExpirationPolicy getSessionExpirationPolicy() {
		return this.sessionExpirationPolicy;
	}

	@Nullable @Override
	public ExpirationAttributes getExpiry(@Nullable Region.Entry<String, Object> regionEntry) {

		return resolveSession(regionEntry)
			.flatMap(getSessionExpirationPolicy()::determineExpirationTimeout)
			.map(expirationTimeout ->
				newExpirationAttributes(expirationTimeout, getSessionExpirationPolicy().getExpirationAction()))
			.orElse(null);
	}

	/**
	 * Constructs a new {@link ExpirationAttributes} initialized with the given {@link Duration expiration timeut}
	 * and default {@link ExpirationAction#INVALIDATE expirtion action}.
	 *
	 * @param expirationTimeout {@link Duration} specifying the expiration timeout.
	 * @return the new {@link ExpirationAttributes}.
	 * @see #newExpirationAttributes(Duration, SessionExpirationPolicy.ExpirationAction)
	 * @see ExpirationAttributes
	 * @see Duration
	 */
	@NonNull
	protected ExpirationAttributes newExpirationAttributes(@NonNull Duration expirationTimeout) {
		return newExpirationAttributes(expirationTimeout, DEFAULT_EXPIRATION_ACTION);
	}

	/**
	 * Constructs a new {@link ExpirationAttributes} initialized with the given {@link Duration expiration timeout}
	 * and action taken when the {@link Session} expires.
	 *
	 * @param expirationTimeout {@link Duration} specifying the expiration timeout.
	 * @param expirationAction action taken when the {@link Session} expires.
	 * @return the new {@link ExpirationAttributes}.
	 * @see #newExpirationAttributes(int, ExpirationAction)
	 * @see SessionExpirationPolicy.ExpirationAction
	 * @see ExpirationAttributes
	 * @see Duration
	 */
	@NonNull
	protected ExpirationAttributes newExpirationAttributes(@NonNull Duration expirationTimeout,
			@Nullable SessionExpirationPolicy.ExpirationAction expirationAction) {

		int expirationTimeoutInSeconds =
			(int) Math.min(Integer.MAX_VALUE, Math.max(expirationTimeout.getSeconds(), 1));

		return newExpirationAttributes(expirationTimeoutInSeconds, toGemFireExpirationAction(expirationAction));
	}

	/**
	 * Constructs a new {@link ExpirationAttributes} initialized with the given {@link Integer expiration timeout}
	 * in seconds and {@link ExpirationAction} taken when the {@link Session} expires.
	 *
	 * @param expirationTimeInSeconds {@link Integer length of time} in seconds until the {@link Session} expires.
	 * @param expirationAction {@link ExpirationAction} taken when the {@link Session} expires.
	 * @return the new {@link ExpirationAttributes}.
	 * @see ExpirationAction
	 * @see ExpirationAttributes
	 */
	@NonNull
	private ExpirationAttributes newExpirationAttributes(int expirationTimeInSeconds,
			ExpirationAction expirationAction) {

		return new ExpirationAttributes(expirationTimeInSeconds, expirationAction);
	}

	/**
	 * Resolves an {@link Optional} {@link Session} object from the {@link Region.Entry#getValue() Region Entry Value}.
	 *
	 * @param regionEntry {@link Region.Entry} from which to extract the {@link Session} value.
	 * @return an {@link Optional} {@link Session} object from the {@link Region.Entry#getValue() Region Entry Value}.
	 * @see Region.Entry
	 * @see Session
	 * @see Optional
	 * @see #resolveSession(Object)
	 */
	private Optional<Session> resolveSession(@Nullable Region.Entry<String, Object> regionEntry) {

		return Optional.ofNullable(regionEntry)
			.map(Region.Entry::getValue)
			.flatMap(this::resolveSession);
	}

	/**
	 * Resolves an {@link Optional} {@link Session} object from the given {@link Object} value.
	 *
	 * The {@link Object} may already be a {@link Session} or may possibly be a {@link PdxInstance}
	 * if Apache Geode/Pivotal GemFire PDX serialization is enabled.
	 *
	 * @param value {@link Object} to evaluate as a {@link Session}.
	 * @return an {@link Optional} {@link Session} from the given {@link Object}.
	 * @see Session
	 * @see PdxInstance
	 * @see Optional
	 * @see Object
	 */
	private Optional<Session> resolveSession(@Nullable Object value) {

		return Optional.ofNullable(value instanceof Session ? (Session) value
			: value instanceof PdxInstance ? (Session) ((PdxInstance) value).getObject()
			: null);
	}

	/**
	 * Converts the {@link ExpirationAction} from the given
	 * {@link SessionExpirationPolicy.ExpirationAction}.
	 *
	 * Defaults to {@link ExpirationAction#INVALIDATE} if {@link SessionExpirationPolicy.ExpirationAction}
	 * is {@literal null}.
	 *
	 * @param expirationAction {@link SessionExpirationPolicy.ExpirationAction} to convert into an
	 * {@link ExpirationAction}.
	 * @return an {@link ExpirationAction} from the given
	 * {@link SessionExpirationPolicy.ExpirationAction}; defaults to {@link ExpirationAction#INVALIDATE}.
	 * @see SessionExpirationPolicy.ExpirationAction
	 * @see ExpirationAction
	 */
	private ExpirationAction toGemFireExpirationAction(SessionExpirationPolicy.ExpirationAction expirationAction) {

		switch (SessionExpirationPolicy.ExpirationAction.defaultIfNull(expirationAction)) {
			case DESTROY:
				return ExpirationAction.DESTROY;
			default:
				return ExpirationAction.INVALIDATE;
		}
	}
}
