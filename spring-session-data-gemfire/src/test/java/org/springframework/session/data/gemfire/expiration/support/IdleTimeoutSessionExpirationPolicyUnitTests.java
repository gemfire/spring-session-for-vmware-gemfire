/*
 * Copyright (c) VMware, Inc. 2022. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.session.data.gemfire.expiration.support.IdleTimeoutSessionExpirationPolicy.DEFAULT_IDLE_TIMEOUT;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import org.springframework.session.Session;

/**
 * Unit tests for {@link IdleTimeoutSessionExpirationPolicy}.
 *
 * @author John Blum
 * @see java.time.Duration
 * @see java.time.Instant
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.session.Session
 * @see org.springframework.session.data.gemfire.expiration.support.IdleTimeoutSessionExpirationPolicy
 * @since 2.1.0
 */
public class IdleTimeoutSessionExpirationPolicyUnitTests {

	@Test
	public void constructDefaultIdleTimeoutExpirationPolicy() {

		IdleTimeoutSessionExpirationPolicy sessionExpirationPolicy = new IdleTimeoutSessionExpirationPolicy();

		assertThat(sessionExpirationPolicy).isNotNull();
		assertThat(sessionExpirationPolicy.getIdleTimeout().orElse(null)).isEqualTo(DEFAULT_IDLE_TIMEOUT);
	}

	@Test
	public void constructNewIdleTimeoutSessionExpirationPolicyWithIdleTimeout() {

		Duration idleExpirationTimeout = Duration.ofMinutes(60L);

		IdleTimeoutSessionExpirationPolicy sessionExpirationPolicy =
			new IdleTimeoutSessionExpirationPolicy(idleExpirationTimeout);

		assertThat(sessionExpirationPolicy).isNotNull();
		assertThat(sessionExpirationPolicy.getIdleTimeout().orElse(null)).isEqualTo(idleExpirationTimeout);
	}

	@Test
	public void constructNewIdleTimeoutSessionExpirationPolicyWithNullIdleTimeout() {

		IdleTimeoutSessionExpirationPolicy sessionExpirationPolicy = new IdleTimeoutSessionExpirationPolicy(null);

		assertThat(sessionExpirationPolicy).isNotNull();
		assertThat(sessionExpirationPolicy.getIdleTimeout().orElse(null)).isNull();
	}

	@Test
	public void determineExpirationTimeoutReturnsExpiredDuration() {

		Duration idleTimeout = Duration.ofSeconds(60L);

		IdleTimeoutSessionExpirationPolicy sessionExpirationPolicy =
			new IdleTimeoutSessionExpirationPolicy(idleTimeout);

		assertThat(sessionExpirationPolicy.getIdleTimeout().orElse(null)).isEqualTo(idleTimeout);

		Session mockSession = mock(Session.class);

		when(mockSession.getLastAccessedTime())
			.thenReturn(Instant.ofEpochMilli(System.currentTimeMillis() - Duration.ofSeconds(61L).toMillis()));

		Duration expirationTimeout = sessionExpirationPolicy.determineExpirationTimeout(mockSession).orElse(null);

		assertThat(expirationTimeout).isNotNull();
		assertThat(expirationTimeout).isLessThan(Duration.ZERO);

		verify(mockSession, never()).getCreationTime();
		verify(mockSession, times(1)).getLastAccessedTime();
	}

	@Test
	public void determineExpirationTimeoutReturnsNonExpiredDuration() {

		Duration idleTimeout = Duration.ofSeconds(60L);

		IdleTimeoutSessionExpirationPolicy sessionExpirationPolicy =
			new IdleTimeoutSessionExpirationPolicy(idleTimeout);

		assertThat(sessionExpirationPolicy.getIdleTimeout().orElse(null)).isEqualTo(idleTimeout);

		Session mockSession = mock(Session.class);

		when(mockSession.getLastAccessedTime())
			.thenReturn(Instant.ofEpochMilli(System.currentTimeMillis() - Duration.ofSeconds(30L).toMillis()));

		Duration expirationTimeout = sessionExpirationPolicy.determineExpirationTimeout(mockSession).orElse(null);

		assertThat(expirationTimeout).isNotNull();
		assertThat(expirationTimeout).isGreaterThan(Duration.ZERO);

		verify(mockSession, never()).getCreationTime();
		verify(mockSession, times(1)).getLastAccessedTime();
	}

	@Test
	public void determineExpirationTimeoutWithNoIdleTimeoutConfiguredReturnsNoDuration() {

		IdleTimeoutSessionExpirationPolicy sessionExpirationPolicy = new IdleTimeoutSessionExpirationPolicy(null);

		assertThat(sessionExpirationPolicy.getIdleTimeout().orElse(null)).isNull();

		Session mockSession = mock(Session.class);

		Duration expirationTimeout = sessionExpirationPolicy.determineExpirationTimeout(mockSession).orElse(null);

		assertThat(expirationTimeout).isNull();

		verify(mockSession, never()).getCreationTime();
		verify(mockSession, never()).getLastAccessedTime();
	}

	@Test
	public void expirationTimeoutIsNotGreaterThanIdleTimeout() {

		Duration idleTimeout = Duration.ofSeconds(60L);

		IdleTimeoutSessionExpirationPolicy sessionExpirationPolicy =
			new IdleTimeoutSessionExpirationPolicy(idleTimeout);

		assertThat(sessionExpirationPolicy.getIdleTimeout().orElse(null)).isEqualTo(idleTimeout);

		Session mockSession = mock(Session.class);

		when(mockSession.getLastAccessedTime())
			.thenReturn(Instant.ofEpochMilli(System.currentTimeMillis() + Duration.ofSeconds(60L).toMillis()));

		Duration expirationTimeout = sessionExpirationPolicy.determineExpirationTimeout(mockSession).orElse(null);

		assertThat(expirationTimeout).isNotNull();
		assertThat(expirationTimeout).isEqualTo(idleTimeout);

		verify(mockSession, never()).getCreationTime();
		verify(mockSession, times(1)).getLastAccessedTime();
	}
}
