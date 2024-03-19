/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.session.Session;

/**
 * Unit Tests for {@link SessionChangedEvent}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.session.Session
 * @since 2.2.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionChangedEventUnitTests {

	@Mock
	private Session mockSession;

	@Test
	public void constructSessionChangedEvent() {

		Object source = new Object();

		SessionChangedEvent event = new SessionChangedEvent(source, this.mockSession);

		assertThat(event).isNotNull();
		assertThat(event.getSource()).isEqualTo(source);
		assertThat(event.<Session>getSession()).isEqualTo(this.mockSession);
	}
}
