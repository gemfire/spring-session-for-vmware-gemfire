/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.support;

import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.util.StringUtils;

/**
 * Abstract utility class containing functions for managing {@link Session} objects.
 *
 * @author John Blum
 * @see org.springframework.session.Session
 * @since 2.1.2
 */
public abstract class SessionUtils {

	/**
	 * Determines whether the given {@link Object Session ID} is valid.
	 *
	 * @param sessionId {@link Object ID} of a {@link Session} to evaluate.
	 * @return a boolean value indicating whether the given {@link Object Session ID} is valid.
	 */
	public static boolean isValidSessionId(@Nullable Object sessionId) {

		return Optional.ofNullable(sessionId)
			.map(Object::toString)
			.filter(StringUtils::hasText)
			.isPresent();
	}
}
