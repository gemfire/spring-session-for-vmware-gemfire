/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.expiration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy.ExpirationAction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit Tests for {@link SessionExpirationPolicy}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy
 * @see org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy.ExpirationAction
 * @since 2.1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionExpirationPolicyUnitTests {

	@Mock
	private SessionExpirationPolicy mockSessionExpirationPolicy;

	@Test
	public void expirationActionDefaultsToInvalidate() {

		when(this.mockSessionExpirationPolicy.getExpirationAction()).thenCallRealMethod();

		assertThat(this.mockSessionExpirationPolicy.getExpirationAction())
			.isEqualTo(SessionExpirationPolicy.ExpirationAction.INVALIDATE);

		verify(this.mockSessionExpirationPolicy, times(1)).getExpirationAction();
	}

	@Test
	public void expirationActionDefaultIfNullReturnsDefault() {
		assertThat(ExpirationAction.defaultIfNull(null)).isEqualTo(ExpirationAction.INVALIDATE);
	}

	@Test
	public void expirationActionDefaultIfNullReturnsExpirationAction() {
		assertThat(ExpirationAction.defaultIfNull(ExpirationAction.DESTROY)).isEqualTo(ExpirationAction.DESTROY);
	}
}
