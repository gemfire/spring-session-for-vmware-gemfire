/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.data.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.DataInput;
import java.io.DataOutput;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;

/**
 * Unit Tests for {@link DataSerializerSessionSerializerAdapter}.
 *
 * @author John Blum
 * @see java.io.DataInput
 * @see java.io.DataOutput
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.session.Session
 * @see org.springframework.session.data.gemfire.serialization.SessionSerializer
 * @see org.springframework.session.data.gemfire.serialization.data.support.DataSerializerSessionSerializerAdapter
 * @since 2.0.0
 */
public class DataSerializerSessionSerializerAdapterUnitTests extends IntegrationTestsSupport {

	// TODO: Figure out why this is necessary for Gradle but not when running tests in the IDE!
	@BeforeClass
	public static void unregisterAllDataSerializersBeforeTests() {
		unregisterAllDataSerializers();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void setAndGetSessionSerializerReturnsExpected() {

		SessionSerializer<Session, DataInput, DataOutput> mockSessionSerializer = mock(SessionSerializer.class);

		DataSerializerSessionSerializerAdapter<Session> dataSerializer =
			new DataSerializerSessionSerializerAdapter<>();

		dataSerializer.setSessionSerializer(mockSessionSerializer);

		assertThat(dataSerializer.getSessionSerializer()).isSameAs(mockSessionSerializer);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setSessionSerializerToNullThrowsIllegalArgumentException() {

		try {
			new DataSerializerSessionSerializerAdapter<>().setSessionSerializer(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("SessionSerializer is required");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void getUninitializedSessionSerializerThrowsIllegalStateException() {

		try {
			new DataSerializerSessionSerializerAdapter<>().getSessionSerializer();
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("SessionSerializer was not properly configured");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void serializeDelegatesToSessionSerializerSerialize() {

		DataOutput mockDataOutput = mock(DataOutput.class);

		Session mockSession = mock(Session.class);

		SessionSerializer<Session, DataInput, DataOutput> mockSessionSerializer = mock(SessionSerializer.class);

		DataSerializerSessionSerializerAdapter<Session> dataSerializer = new DataSerializerSessionSerializerAdapter<>();

		dataSerializer.setSessionSerializer(mockSessionSerializer);

		assertThat(dataSerializer.getSessionSerializer()).isSameAs(mockSessionSerializer);

		dataSerializer.serialize(mockSession, mockDataOutput);

		verify(mockSessionSerializer, times(1)).serialize(eq(mockSession), eq(mockDataOutput));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void deserializeDelegatesToSessionSerializerDeserialize() {

		DataInput mockDataInput = mock(DataInput.class);

		Session mockSession = mock(Session.class);

		SessionSerializer<Session, DataInput, DataOutput> mockSessionSerializer = mock(SessionSerializer.class);

		when(mockSessionSerializer.deserialize(any(DataInput.class))).thenReturn(mockSession);

		DataSerializerSessionSerializerAdapter<Session> dataSerializer = new DataSerializerSessionSerializerAdapter<>();

		dataSerializer.setSessionSerializer(mockSessionSerializer);

		assertThat(dataSerializer.getSessionSerializer()).isSameAs(mockSessionSerializer);
		assertThat(dataSerializer.deserialize(mockDataInput)).isEqualTo(mockSession);

		verify(mockSessionSerializer, times(1)).deserialize(eq(mockDataInput));
	}
}
