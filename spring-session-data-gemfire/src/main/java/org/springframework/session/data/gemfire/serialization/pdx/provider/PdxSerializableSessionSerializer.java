/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.pdx.provider;

import static org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSession;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxWriter;

import org.springframework.session.Session;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;
import org.springframework.session.data.gemfire.serialization.pdx.AbstractPdxSerializableSessionSerializer;
import org.springframework.session.data.gemfire.support.AbstractSession;

/**
 * The {@link PdxSerializableSessionSerializer} class is an implementation of the {@link SessionSerializer} interface
 * used to serialize a Spring {@link Session} using the GemFire/Geode's PDX Serialization framework.
 *
 * @author John Blum
 * @see Duration
 * @see Instant
 * @see PdxReader
 * @see PdxWriter
 * @see Session
 * @see SessionSerializer
 * @see AbstractPdxSerializableSessionSerializer
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class PdxSerializableSessionSerializer extends AbstractPdxSerializableSessionSerializer<GemFireSession> {

	@Override
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	public void serialize(GemFireSession session, PdxWriter writer) {

		synchronized (session) {
			writer.writeString("id", session.getId());
			writer.writeLong("creationTime", session.getCreationTime().toEpochMilli());
			writer.writeLong("lastAccessedTime", session.getLastAccessedTime().toEpochMilli());
			writer.writeLong("maxInactiveIntervalInSeconds", session.getMaxInactiveInterval().getSeconds());
			writer.writeString("principalName", session.getPrincipalName());
			writer.writeObject("attributes", newMap(session.getAttributes()));
			writer.markIdentityField("id");
		}
	}

	protected <K, V> Map<K, V> newMap(Map<K, V> map) {
		return new HashMap<>(map);
	}

	@Override
	@SuppressWarnings("unchecked")
	public GemFireSession deserialize(PdxReader reader) {

		GemFireSession session = GemFireSession.from(new AbstractSession() {

			@Override
			public String getId() {
				return reader.readString("id");
			}

			@Override
			public Instant getCreationTime() {
				return Instant.ofEpochMilli(reader.readLong("creationTime"));
			}

			@Override
			public Instant getLastAccessedTime() {
				return Instant.ofEpochMilli(reader.readLong("lastAccessedTime"));
			}

			@Override
			public Duration getMaxInactiveInterval() {
				return Duration.ofSeconds(reader.readLong("maxInactiveIntervalInSeconds"));
			}

			@Override
			public Set<String> getAttributeNames() {
				return Collections.emptySet();
			}
		});

		session.setPrincipalName(reader.readString("principalName"));
		session.getAttributes().from((Map<String, Object>) reader.readObject("attributes"));

		return session;
	}

	@Override
	public boolean canSerialize(Class<?> type) {
		return Optional.ofNullable(type).map(GemFireSession.class::isAssignableFrom).orElse(false);
	}
}
