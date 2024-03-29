/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.support;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import java.util.Optional;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.Region;

import org.springframework.session.Session;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link SessionIdHolder} class is a Spring Session {@link Session} implementation that only holds
 * the {@link String ID} of the {@link Session}.
 *
 * This implementation is only used in case Apache Geode or Pivotal GemFire returns a {@literal null} (old) value
 * in a {@link Region} {@link EntryEvent} triggered by a {@link Operation#DESTROY} or {@link Operation#INVALIDATE}
 * operation.
 *
 * @author John Blum
 * @see EntryEvent
 * @see Operation
 * @see Region
 * @see SessionIdHolder
 * @since 2.0.0
 */
public final class SessionIdHolder extends AbstractSession {

	private final String sessionId;

	/***
	 * Factory method to create an instance of the {@link SessionIdHolder} initialized with
	 * the given {@link String session ID}.
	 *
	 * @param sessionId {@link String} containing the session ID used to initialize
	 * the new instance of {@link SessionIdHolder}.
	 * @return a new instance of {@link SessionIdHolder} initialized with
	 * the given {@link String session ID}.
	 * @throws IllegalArgumentException if session ID is {@literal null} or empty.
	 * @see #SessionIdHolder(String)
	 */
	public static SessionIdHolder create(String sessionId) {
		return new SessionIdHolder(sessionId);
	}

	/**
	 * Constructs a new instance of the {@link SessionIdHolder} initialized with
	 * the given {@link String session ID}.
	 *
	 * @param sessionId {@link String} containing the session ID used to initialize
	 * the new instance of {@link SessionIdHolder}.
	 * @throws IllegalArgumentException if session ID is {@literal null} or empty.
	 */
	public SessionIdHolder(String sessionId) {

		this.sessionId = Optional.ofNullable(sessionId)
			.filter(StringUtils::hasText)
			.orElseThrow(() -> newIllegalArgumentException("Session ID [%s] is required", sessionId));
	}

	/**
	 * Returns the {@link String ID} of this {@link Session}.
	 *
	 * @return the {@link String ID} of this {@link Session}.
	 */
	@Override
	public String getId() {
		return this.sessionId;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Session)) {
			return false;
		}

		Session that = (Session) obj;

		return ObjectUtils.nullSafeEquals(this.getId(), that.getId());
	}

	@Override
	public int hashCode() {

		int hashValue = 17;

		hashValue = 37 * hashValue + ObjectUtils.nullSafeHashCode(getId());

		return hashValue;
	}

	@Override
	public String toString() {
		return getId();
	}
}
