/*
 * Copyright (c) VMware, Inc. 2022. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.support;

import java.util.Map;

import org.apache.geode.cache.Region;

import org.springframework.data.gemfire.GemfireOperations;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository;

/**
 * Framework supporting class for {@link AbstractGemFireOperationsSessionRepository} implementations.
 *
 * By default, all {@link SessionRepository} data access operations throw an {@link UnsupportedOperationException}.
 * Therefore, you are free to implement only the {@link SessionRepository} data access operations you need.
 *
 * For instance, if you only want to implement a {@literal read-only} {@link SessionRepository}, then you can simply
 * {@literal override} the {@link #findById(String)}, {@link #findByIndexNameAndIndexValue(String, String)}
 * and {@link #findByPrincipalName(String)} Repository methods.  In that way, the {@link Session} can never be updated.
 *
 * @author John Blum
 * @see Region
 * @see GemfireOperations
 * @see Session
 * @see AbstractGemFireOperationsSessionRepository
 * @since 2.1.1
 */
@SuppressWarnings("unused")
public abstract class GemFireOperationsSessionRepositorySupport
		extends AbstractGemFireOperationsSessionRepository {

	private static final String OPERATION_NOT_SUPPORTED = "Operation Not Supported";

	/**
	 * Construct an uninitialized instance of {@link GemFireOperationsSessionRepositorySupport}.
	 *
	 * @see #GemFireOperationsSessionRepositorySupport(GemfireOperations)
	 */
	protected GemFireOperationsSessionRepositorySupport() { }

	/**
	 * Constructs a new instance of {@link GemFireOperationsSessionRepositorySupport} initialized with
	 * the given {@link GemfireOperations} object used to perform {@link Region} data access operations
	 * managing the {@link Session} state.
	 *
	 * @param gemfireOperations {@link GemfireOperations} for performing data access operations
	 * on the {@link Region} used to manage {@link Session} state.
	 * @see GemfireOperations
	 */
	public GemFireOperationsSessionRepositorySupport(GemfireOperations gemfireOperations) {
		super(gemfireOperations);
	}

	@Override
	public Session createSession() {
		throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
	}

	@Override
	public void deleteById(String id) {
		throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
	}

	@Override
	public Session findById(String id) {
		throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
	}

	@Override
	public Map<String, Session> findByIndexNameAndIndexValue(String indexName, String indexValue) {
		throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
	}

	@Override
	public void save(Session session) {
		throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
	}
}
