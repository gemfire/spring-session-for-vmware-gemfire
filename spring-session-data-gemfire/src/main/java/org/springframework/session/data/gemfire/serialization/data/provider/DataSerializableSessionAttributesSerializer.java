/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization.data.provider;

import static org.springframework.data.gemfire.util.ArrayUtils.asArray;
import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeSet;
import static org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.DeltaCapableGemFireSessionAttributes;
import static org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSessionAttributes;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Set;

import org.apache.geode.DataSerializer;

import org.springframework.session.Session;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;
import org.springframework.session.data.gemfire.serialization.data.AbstractDataSerializableSessionSerializer;

/**
 * The {@link DataSerializableSessionAttributesSerializer} class is an implementation of the {@link SessionSerializer}
 * interface used to serialize a Spring {@link Session} attributes using the GemFire/Geode's Data Serialization
 * framework.
 *
 * @author John Blum
 * @see DataInput
 * @see DataOutput
 * @see DataSerializer
 * @see Session
 * @see DeltaCapableGemFireSessionAttributes
 * @see GemFireSessionAttributes
 * @see SessionSerializer
 * @see AbstractDataSerializableSessionSerializer
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class DataSerializableSessionAttributesSerializer
		extends AbstractDataSerializableSessionSerializer<GemFireSessionAttributes> {

	/**
	 * Register custom Spring Session {@link DataSerializer DataSerializers} with Apache Geode/Pivotal GemFire
	 * to handle de/serialization of Spring Session, {@link Session} attribute types.
	 *
	 * @see DataSerializer#register(Class)
	 */
	public static void register() {
		register(DataSerializableSessionAttributesSerializer.class);
	}

	/**
	 * Returns the identifier for this {@link DataSerializer}.
	 *
	 * @return the identifier for this {@link DataSerializer}.
	 */
	@Override
	public int getId() {
		return 0x8192ACE5;
	}

	/**
	 * Returns the {@link Class types} supported and handled by this {@link DataSerializer} during de/serialization.
	 *
	 * @return the {@link Class types} supported and handled by this {@link DataSerializer} during de/serialization.
	 * @see DeltaCapableGemFireSessionAttributes
	 * @see GemFireSessionAttributes
	 * @see Class
	 */
	@Override
	public Class<?>[] getSupportedClasses() {
		return asArray(GemFireSessionAttributes.class, DeltaCapableGemFireSessionAttributes.class);
	}

	@Override
	public void serialize(GemFireSessionAttributes sessionAttributes, DataOutput out) {

		synchronized (sessionAttributes.getLock()) {

			Set<String> attributeNames = nullSafeSet(sessionAttributes.getAttributeNames());

			safeWrite(out, output -> output.writeInt(attributeNames.size()));

			attributeNames.forEach(attributeName -> {
				safeWrite(out, output -> output.writeUTF(attributeName));
				safeWrite(out, output -> serializeObject(sessionAttributes.getAttribute(attributeName), output));
			});
		}
	}

	@Override
	public GemFireSessionAttributes deserialize(DataInput in) {

		GemFireSessionAttributes sessionAttributes = GemFireSessionAttributes.create();

		for (int count = safeRead(in, DataInput::readInt); count > 0; count--) {
			sessionAttributes.setAttribute(safeRead(in, DataInput::readUTF), safeRead(in, this::deserializeObject));
		}

		return sessionAttributes;
	}
}
