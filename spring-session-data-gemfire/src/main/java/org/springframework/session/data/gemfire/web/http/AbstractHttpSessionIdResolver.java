/*
 * Copyright (c) VMware, Inc. 2022. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.web.http;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newUnsupportedOperationException;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.session.web.http.HttpSessionIdResolver;

/**
 * Abstract base class implementing the Spring Session core {@link HttpSessionIdResolver} interface to encapsulate
 * functionality common to all implementations as well as to simplify the implementation of the Spring Session core
 * {@link HttpSessionIdResolver} interface.
 *
 * @author John Blum
 * @see HttpServletRequest
 * @see HttpServletResponse
 * @see HttpSessionIdResolver
 * @since 2.5.0
 */
public abstract class AbstractHttpSessionIdResolver implements HttpSessionIdResolver {

	private static final String NOT_IMPLEMENTED = "NOT IMPLEMENTED";

	/**
	 * @inheritDoc
	 */
	@Override
	public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
		throw newUnsupportedOperationException(NOT_IMPLEMENTED);

	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<String> resolveSessionIds(HttpServletRequest request) {
		return Collections.emptyList();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void expireSession(HttpServletRequest request, HttpServletResponse response) {
		throw newUnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
