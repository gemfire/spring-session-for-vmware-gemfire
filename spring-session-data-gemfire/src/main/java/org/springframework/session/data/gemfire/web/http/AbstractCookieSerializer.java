/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.web.http;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newUnsupportedOperationException;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.session.web.http.CookieSerializer;

/**
 * Abstract base class implementing the Spring Session core {@link CookieSerializer} interface to encapsulate
 * functionality common to all implementations as well as to simplify the implementation of the Spring Session core
 * {@link CookieSerializer} interface.
 *
 * @author John Blum
 * @see HttpServletRequest
 * @see CookieSerializer
 * @since 2.5.0
 */
public class AbstractCookieSerializer implements CookieSerializer {

	protected static final String NOT_IMPLEMENTED = "NOT IMPLEMENTED";

	/**
	 * @inheritDoc
	 */
	@Override
	public List<String> readCookieValues(HttpServletRequest request) {
		return Collections.emptyList();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void writeCookieValue(CookieValue cookieValue) {
		throw newUnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
