/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization;

import java.util.Optional;

/**
 * The {@link SessionSerializer} interface is a Service Provider Interface (SPI) for providers
 * needing to provide a custom implementation of their serialization strategy.
 *
 * @author John Blum
 * @since 2.0.0
 */
public interface SessionSerializer<T, IN, OUT> {

	/**
	 * Serializes the given {@link Object} to the provided {@code out} stream.
	 *
	 * @param session {@link Object} to serialize.
	 * @param out stream in which to write the bytes of the {@link Object}.
	 */
	void serialize(T session, OUT out);

	/**
	 * Deserializes an {@link Object} from bytes contained in the provided {@code in} stream.
	 *
	 * @param in stream from which to read the bytes of the {@link Object}.
	 * @return the deserialized {@link Object}.
	 */
	T deserialize(IN in);

	/**
	 * Determines whether the given {@link Class type} can be de/serialized by this {@link SessionSerializer}.
	 *
	 * @param type {@link Class} to evaluate for whether de/serialization is supported.
	 * @return a boolean value indicating whether the specified {@link Class type} can be de/serialized
	 * by this {@link SessionSerializer}.
	 * @see #canSerialize(Object)
	 */
	boolean canSerialize(Class<?> type);

	/**
	 * Determines whether the given {@link Object} can be de/serialized by this {@link SessionSerializer}.
	 *
	 * @param obj {@link Object} to evaluate for whether de/serialization is supported.
	 * @return a boolean value indicating whether the specified {@link Object} can be de/serialized
	 * by this {@link SessionSerializer}.
	 * @see #canSerialize(Class)
	 */
	default boolean canSerialize(Object obj) {

		return Optional.ofNullable(obj)
			.map(Object::getClass)
			.filter(this::canSerialize)
			.isPresent();
	}
}
