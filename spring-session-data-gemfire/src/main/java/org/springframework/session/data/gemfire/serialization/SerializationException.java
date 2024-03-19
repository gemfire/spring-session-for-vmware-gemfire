/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization;

/**
 * The SerializationException class is a {@link RuntimeException} indicating an error occurred while attempting to
 * serialize a {@link org.springframework.session.Session}.
 *
 * @author John Blum
 * @see java.lang.RuntimeException
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class SerializationException extends RuntimeException {

	/**
	 * Constructs a default instance of {@link SerializationException} with no {@link String message}
	 * or {@link Throwable cause}.
	 *
	 * @see java.lang.RuntimeException#RuntimeException()
	 */
	public SerializationException() { }

	/**
	 * Constructs a new instance of {@link SerializationException} initialized with the given {@link String message}
	 * describing the serialization error.
	 *
	 * @param message {@link String} describing the serialization error.
	 * @see java.lang.RuntimeException#RuntimeException(String)
	 * @see java.lang.String
	 */
	public SerializationException(String message) {
		super(message);
	}

	/**
	 * Constructs a new instance of {@link SerializationException} initialized with the given {@link Throwable cause}
	 * of the serialization error.
	 *
	 * @param cause {@link Throwable underlying cause} of the serialization error.
	 * @see java.lang.RuntimeException#RuntimeException(Throwable)
	 * @see java.lang.Throwable
	 */
	public SerializationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new instance of {@link SerializationException} initialized with the given {@link String message}
	 * describing the serialization error and {@link Throwable cause} of the serialization error.
	 *
	 * @param message {@link String} describing the serialization error.
	 * @param cause {@link Throwable underlying cause} of the serialization error.
	 * @see java.lang.RuntimeException#RuntimeException(String, Throwable)
	 * @see java.lang.Throwable
	 * @see java.lang.String
	 */
	public SerializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
