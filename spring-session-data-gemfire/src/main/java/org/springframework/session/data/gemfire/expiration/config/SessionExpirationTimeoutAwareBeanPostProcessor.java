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
import org.springframework.util.Assert;

/**
 * The {@link SessionExpirationTimeoutAwareBeanPostProcessor} class is a Spring {@link BeanPostProcessor} handling
 * the post processing of all Spring beans defined in the Spring container implementing
 * the {@link SessionExpirationTimeoutAware} interface.
 *
 * @author John Blum
 * @see BeanPostProcessor
 * @since 2.1.0
 */
public class SessionExpirationTimeoutAwareBeanPostProcessor implements BeanPostProcessor {

	private final Duration expirationTimeout;

	/**
	 * Constructs a new {@link SessionExpirationTimeoutAwareBeanPostProcessor} initialized with
	 * the given {@link Session} {@link Duration expiration timeout}.
	 *
	 * @param expirationTimeout {@link Duration} specifying the length of time until {@link Session} expires.
	 * @throws IllegalArgumentException if {@link Duration} is {@literal null}.
	 * @see Duration
	 */
	public SessionExpirationTimeoutAwareBeanPostProcessor(Duration expirationTimeout) {

		Assert.notNull(expirationTimeout, "Expiration timeout is required");

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

	@Nullable @Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		if (bean instanceof SessionExpirationTimeoutAware) {
			((SessionExpirationTimeoutAware) bean).setExpirationTimeout(getExpirationTimeout());
		}

		return bean;
	}
}
