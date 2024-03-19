/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.support;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.springframework.session.Session;

/**
 * Abstract base class for implementations of the {@link Session} interface in order to simplify the implementation
 * of various {@link Session} types and their capabilities.
 *
 * @author John Blum
 * @see Duration
 * @see Instant
 * @see Session
 * @since 2.0.0
 */
public class AbstractSession implements Session {

	public static final String NOT_IMPLEMENTED = "Not Implemented";

	@Override
	public String getId() {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public void setAttribute(String attributeName, Object attributeValue) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public <T> T getAttribute(String attributeName) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public <T> T getAttributeOrDefault(String name, T defaultValue) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public <T> T getRequiredAttribute(String name) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public Set<String> getAttributeNames() {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public boolean isExpired() {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public Instant getCreationTime() {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public void setLastAccessedTime(Instant lastAccessedTime) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public Instant getLastAccessedTime() {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public void setMaxInactiveInterval(Duration interval) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public Duration getMaxInactiveInterval() {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	@Override
	public void removeAttribute(String attributeName) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
