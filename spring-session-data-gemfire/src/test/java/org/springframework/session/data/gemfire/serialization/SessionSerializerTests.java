/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit Tests for the {@link SessionSerializer} interface.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.springframework.session.data.gemfire.serialization.SessionSerializer
 * @since 2.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionSerializerTests {

	@Mock
	@SuppressWarnings("rawtypes")
	private SessionSerializer sessionSerializer;

	@Test
	@SuppressWarnings("unchecked")
	public void canSerializeWithSerializableObjectReturnsTrue() {

		when(this.sessionSerializer.canSerialize(any(Class.class))).thenReturn(true);
		when(this.sessionSerializer.canSerialize(any(Object.class))).thenCallRealMethod();

		assertThat(this.sessionSerializer.canSerialize("test")).isTrue();

		verify(this.sessionSerializer, times(1)).canSerialize(eq("test"));
		verify(this.sessionSerializer, times(1)).canSerialize(eq(String.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canSerializeWithNullReturnFalse() {

		assertThat(this.sessionSerializer.canSerialize((Object) null)).isFalse();

		verify(this.sessionSerializer, times(1)).canSerialize(eq((Object) null));
		verify(this.sessionSerializer, never()).canSerialize(any(Class.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canSerializeWithNonSerializableObjectReturnFalse() {

		when(this.sessionSerializer.canSerialize(any(Class.class))).thenReturn(false);
		when(this.sessionSerializer.canSerialize(any(Object.class))).thenCallRealMethod();

		assertThat(this.sessionSerializer.canSerialize("test")).isFalse();

		verify(this.sessionSerializer, times(1)).canSerialize(eq("test"));
		verify(this.sessionSerializer, times(1)).canSerialize(eq(String.class));
	}
}
