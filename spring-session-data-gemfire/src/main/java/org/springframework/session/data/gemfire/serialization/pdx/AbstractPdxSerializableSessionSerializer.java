/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization.pdx;

import java.util.Optional;

import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.PdxWriter;

import org.springframework.session.Session;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;

/**
 * The {@link AbstractPdxSerializableSessionSerializer} class is an abstract base class containing functionality common
 * to all GemFire/Geode PDX-based {@link SessionSerializer} implementations.
 *
 * This class also implements GemFire/Geode's {@link PdxSerializer} interface, adapting it to the Spring Session,
 * Data Pivotal GemFire {@link SessionSerializer} interface.
 *
 * @author John Blum
 * @see org.apache.geode.pdx.PdxReader
 * @see org.apache.geode.pdx.PdxWriter
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.session.Session
 * @see org.springframework.session.data.gemfire.serialization.SessionSerializer
 * @since 2.0.0
 */
public abstract class AbstractPdxSerializableSessionSerializer<T extends Session>
		implements PdxSerializer, SessionSerializer<T, PdxReader, PdxWriter> {

	@Override
	@SuppressWarnings("unchecked")
	public boolean toData(Object session, PdxWriter writer) {

		return Optional.ofNullable(session)
			.filter(this::canSerialize)
			.map(it -> {
				serialize((T) session, writer);
				return true;
			})
			.orElse(false);
	}

	@Override
	public Object fromData(Class<?> type, PdxReader reader) {

		return Optional.ofNullable(type)
			.filter(this::canSerialize)
			.map(it -> deserialize(reader))
			.orElse(null);
	}

	@Override
	public boolean canSerialize(Class<?> type) {
		return Optional.ofNullable(type).map(Session.class::isAssignableFrom).orElse(false);
	}
}
