/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.config;

import java.time.Duration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.expiration.repository.FixedDurationExpirationSessionRepository;

/**
 * The {@link FixedDurationExpirationSessionRepositoryBeanPostProcessor} class wraps an existing, data store specific,
 * instance of {@link SessionRepository} in an instance of {@link FixedDurationExpirationSessionRepository} initialized
 * with a provided {@link Duration} for the expiration timeout to implement lazy, fixed {@link Duration} expiration
 * on all {@link Session Sessions}.
 *
 * @author John Blum
 * @see BeanPostProcessor
 * @see Session
 * @see SessionRepository
 * @see FixedDurationExpirationSessionRepository
 * @see <a href="https://github.com/spring-projects/spring-session/issues/922">Absolute Session Timeouts</a>
 * @since 2.1.0
 */
@SuppressWarnings("unused")
public class FixedDurationExpirationSessionRepositoryBeanPostProcessor implements BeanPostProcessor {

	private final Duration expirationTimeout;

	/**
	 * Constructs a new instance of {@link FixedDurationExpirationSessionRepositoryBeanPostProcessor} initialized with
	 * the given {@link Duration} to implement lazy, fixed {@link Duration} expiration policy
	 * on all {@link Session Sessions}.
	 *
	 * @param expirationTimeout {@link Duration} indicating the length of time until the {@link Session} expires.
	 * @see Duration
	 */
	public FixedDurationExpirationSessionRepositoryBeanPostProcessor(@Nullable Duration expirationTimeout) {
		this.expirationTimeout = expirationTimeout;
	}

	/**
	 * Returns the configured {@link Session} {@link Duration expiration timeout}.
	 *
	 * @return the configured {@link Session} {@link Duration expiration timeout}.
	 * @see Duration
	 */
	protected Duration getExpirationTimeout() {
		return this.expirationTimeout;
	}

	@Nullable @Override @SuppressWarnings("unchecked")
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		return bean instanceof SessionRepository
			? new FixedDurationExpirationSessionRepository<>((SessionRepository) bean, getExpirationTimeout())
			: bean;
	}
}
