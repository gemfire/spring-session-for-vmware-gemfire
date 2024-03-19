/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.pdx.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.DeltaCapableGemFireSession;
import static org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSession;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxWriter;

import org.springframework.session.FindByIndexNameSessionRepository;

/**
 * Unit Tests for {@link PdxSerializableSessionSerializer}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.session.data.gemfire.serialization.pdx.provider.PdxSerializableSessionSerializer
 * @since 2.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class PdxSerializableSessionSerializerTests {

	@Mock
	private PdxReader mockPdxReader;

	@Mock
	private PdxWriter mockPdxWriter;

	private final PdxSerializableSessionSerializer sessionSerializer = new PdxSerializableSessionSerializer();

	@Test
	public void serializeSessionIsCorrect() {

		GemFireSession<?> session = GemFireSession.create();

		session.setMaxInactiveInterval(Duration.ofMinutes(30));
		session.setAttribute("attributeOne", "valueOne");
		session.setAttribute("attributeTwo", "valueTwo");

		this.sessionSerializer.serialize(session, this.mockPdxWriter);

		verify(this.mockPdxWriter, times(1))
			.writeString(eq("id"), eq(session.getId()));

		verify(this.mockPdxWriter, times(1))
			.writeLong(eq("creationTime"), eq(session.getCreationTime().toEpochMilli()));

		verify(this.mockPdxWriter, times(1))
			.writeLong(eq("lastAccessedTime"), eq(session.getLastAccessedTime().toEpochMilli()));

		verify(this.mockPdxWriter, times(1))
			.writeLong(eq("maxInactiveIntervalInSeconds"), eq(session.getMaxInactiveInterval().getSeconds()));

		verify(this.mockPdxWriter, times(1))
			.writeString(eq("principalName"), eq(session.getPrincipalName()));

		verify(this.mockPdxWriter, times(1))
			.writeObject(eq("attributes"), eq(new HashMap<>(session.getAttributes())));

		verify(this.mockPdxWriter, times(1)).markIdentityField(eq("id"));
	}

	@Test
	public void newMapCopiesMap() {

		Map<String, String> mapCopy = this.sessionSerializer.newMap(Collections.singletonMap("testKey", "testValue"));

		assertThat(mapCopy).isInstanceOf(HashMap.class);
		assertThat(mapCopy).hasSize(1);
		assertThat(mapCopy).containsKey("testKey");
		assertThat(mapCopy.get("testKey")).isEqualTo("testValue");
	}

	@Test
	public void deserializeSessionIsCorrect() {

		Duration expectedMaxInactiveInterval = Duration.ofMinutes(30);

		Instant expectedCreationTime = Instant.now();
		Instant expectedLastAccessedTime = Instant.now();

		Map<String, String> expectedAttributes = new HashMap<>(2);

		expectedAttributes.put("attributeOne", "valueOne");
		expectedAttributes.put("attributeTwo", "valueTwo");
		expectedAttributes.put(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, "jonDoe");

		when(this.mockPdxReader.readString(eq("id"))).thenReturn("123");
		when(this.mockPdxReader.readLong(eq("creationTime"))).thenReturn(expectedCreationTime.toEpochMilli());

		when(this.mockPdxReader.readLong(eq("lastAccessedTime")))
			.thenReturn(expectedLastAccessedTime.toEpochMilli());

		when(this.mockPdxReader.readLong(eq("maxInactiveIntervalInSeconds")))
			.thenReturn(expectedMaxInactiveInterval.getSeconds());

		when(this.mockPdxReader.readString(eq("principalName"))).thenReturn("jonDoe");
		when(this.mockPdxReader.readObject(eq("attributes"))).thenReturn(expectedAttributes);

		GemFireSession<?> session = this.sessionSerializer.deserialize(this.mockPdxReader);

		assertThat(session).isNotNull();
		assertThat(session.getId()).isEqualTo("123");
		// TODO: Problem on Java 17
		// assertThat(session.getCreationTime()).isEqualTo(expectedCreationTime);
		assertThat(session.getCreationTime().toEpochMilli()).isEqualTo(expectedCreationTime.toEpochMilli());
		//assertThat(session.getLastAccessedTime()).isEqualTo(expectedLastAccessedTime);
		assertThat(session.getLastAccessedTime().toEpochMilli()).isEqualTo(expectedLastAccessedTime.toEpochMilli());
		assertThat(session.getMaxInactiveInterval()).isEqualTo(expectedMaxInactiveInterval);
		assertThat(session.getPrincipalName()).isEqualTo("jonDoe");
		assertThat(this.sessionSerializer.newMap(session.getAttributes())).isEqualTo(expectedAttributes);

		verify(this.mockPdxReader, times(1)).readString(eq("id"));
		verify(this.mockPdxReader, times(1)).readLong(eq("creationTime"));
		verify(this.mockPdxReader, times(1)).readLong(eq("lastAccessedTime"));
		verify(this.mockPdxReader, times(1)).readLong(eq("maxInactiveIntervalInSeconds"));
		verify(this.mockPdxReader, times(1)).readString(eq("principalName"));
		verify(this.mockPdxReader, times(1)).readObject(eq("attributes"));
	}

	@Test
	public void canSerializeGemFireSessionClassTypeIsTrue() {
		assertThat(this.sessionSerializer.canSerialize(GemFireSession.class)).isTrue();
	}

	@Test
	public void canSerializeDeltaCapableGemFireSessionClassTypeIsTrue() {
		assertThat(this.sessionSerializer.canSerialize(DeltaCapableGemFireSession.class)).isTrue();
	}

	@Test
	public void canSerializeObjectClassTypeIsFalse() {
		assertThat(this.sessionSerializer.canSerialize(Object.class)).isFalse();
	}

	@Test
	public void canSerializeNullClassTypeIsFalse() {
		assertThat(this.sessionSerializer.canSerialize(null)).isFalse();
	}
}
