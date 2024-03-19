/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit Tests for {@link SessionUtils}.
 *
 * @author John Blum
 * @see org.springframework.session.Session
 * @see org.springframework.session.data.gemfire.support.SessionUtils
 * @since 2.1.2
 */
public class SessionUtilsUnitTests {

	@Test
	public void isValidSessionIdWithValidSessionId() {
		assertThat(SessionUtils.isValidSessionId("1")).isTrue();
	}

	private void testIsValidSessionIdWithInvalidSessionId(Object sessionId) {
		assertThat(SessionUtils.isValidSessionId(sessionId)).isFalse();
	}

	@Test
	public void isValidSessionIdWithEmptySessionId() {
		testIsValidSessionIdWithInvalidSessionId("");
	}

	@Test
	public void isValidSessionIdWithNullSessionId() {
		testIsValidSessionIdWithInvalidSessionId(null);
	}

	@Test
	public void isValidSessionIdWithUnspecifiedSessionId() {
		testIsValidSessionIdWithInvalidSessionId("  ");
	}
}
