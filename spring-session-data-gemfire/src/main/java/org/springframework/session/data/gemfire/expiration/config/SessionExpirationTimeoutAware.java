/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.config;

import java.time.Duration;

import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;

/**
 * The {@link SessionExpirationTimeoutAware} interface is a configuration callback interface allowing implementors
 * to receive a callback with the configured {@link Session} {@link Duration expiration timeout} as set on the
 * {@link EnableGemFireHttpSession} annotation, {@link EnableGemFireHttpSession#maxInactiveIntervalInSeconds()}
 * attribute.
 *
 * @author John Blum
 * @see Duration
 * @see Session
 * @see EnableGemFireHttpSession
 * @since 2.1.0
 */
@SuppressWarnings("unused")
public interface SessionExpirationTimeoutAware {

	/**
	 * Configures the {@link Session} {@link Duration expiration timeout} on this implementing object.
	 *
	 * @param expirationTimeout {@link Duration} specifying the expiration timeout fo the {@link Session}.
	 * @see Duration
	 */
	void setExpirationTimeout(Duration expirationTimeout);

}
