/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization.pdx.support;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import java.util.Optional;

import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.PdxWriter;

import org.springframework.session.Session;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;
import org.springframework.session.data.gemfire.serialization.pdx.AbstractPdxSerializableSessionSerializer;

/**
 * The {@link PdxSerializerSessionSerializerAdapter} class is a two-way Adapter adapting a {@link SessionSerializer}
 * instance as an instance of {@link PdxSerializer} in a GemFire/Geode context, or adapting a {@link PdxSerializer}
 * as a {@link SessionSerializer} in a Spring Session context.
 *
 * @author John Blum
 * @see PdxSerializer
 * @see Session
 * @see SessionSerializer
 * @see AbstractPdxSerializableSessionSerializer
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class PdxSerializerSessionSerializerAdapter<T extends Session>
		extends AbstractPdxSerializableSessionSerializer<T> {

	private final SessionSerializer<T, PdxReader, PdxWriter> sessionSerializer;

	public PdxSerializerSessionSerializerAdapter(SessionSerializer<T, PdxReader, PdxWriter> sessionSerializer) {
		this.sessionSerializer = Optional.ofNullable(sessionSerializer)
			.orElseThrow(() -> newIllegalArgumentException("SessionSerializer is required"));
	}

	public SessionSerializer<T, PdxReader, PdxWriter> getSessionSerializer() {
		return this.sessionSerializer;
	}

	@Override
	public void serialize(T session, PdxWriter writer) {
		getSessionSerializer().serialize(session, writer);
	}

	@Override
	public T deserialize(PdxReader reader) {
		return getSessionSerializer().deserialize(reader);
	}
}
