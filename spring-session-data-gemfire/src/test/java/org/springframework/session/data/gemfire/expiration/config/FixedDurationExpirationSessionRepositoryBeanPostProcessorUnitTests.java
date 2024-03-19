/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.expiration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.Test;

import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.expiration.repository.FixedDurationExpirationSessionRepository;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link FixedDurationExpirationSessionRepositoryBeanPostProcessor}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.session.data.gemfire.expiration.config.FixedDurationExpirationSessionRepositoryBeanPostProcessor
 * @since 2.1.0
 */
public class FixedDurationExpirationSessionRepositoryBeanPostProcessorUnitTests {

	@SuppressWarnings("unchecked")
	private <T> T invokeMethod(Object target, String methodName) throws NoSuchMethodException {

		Method method = target.getClass().getDeclaredMethod(methodName);

		ReflectionUtils.makeAccessible(method);

		return (T) ReflectionUtils.invokeMethod(method, target);
	}

	@Test
	public void constructsFixedDurationExpirationSessionRepositoryBeanPostProcessor() {

		Duration expirationTimeout = Duration.ofMinutes(30L);

		FixedDurationExpirationSessionRepositoryBeanPostProcessor beanPostProcessor =
			new FixedDurationExpirationSessionRepositoryBeanPostProcessor(expirationTimeout);

		assertThat(beanPostProcessor).isNotNull();
		assertThat(beanPostProcessor.getExpirationTimeout()).isEqualTo(expirationTimeout);
	}

	@Test
	public void doesNotProcessNonSessionRepositoryBeans() {

		FixedDurationExpirationSessionRepositoryBeanPostProcessor beanPostProcessor =
			new FixedDurationExpirationSessionRepositoryBeanPostProcessor(null);

		Object bean = beanPostProcessor.postProcessAfterInitialization("test", "testBean");

		assertThat(bean).isNotInstanceOf(SessionRepository.class);
		assertThat(bean).isEqualTo("test");
	}

	@Test
	@SuppressWarnings("all")
	public void processesSessionRepositoryBean() throws Exception {

		Duration expirationTimeout = Duration.ofMinutes(30);

		SessionRepository<?> mockSessionRepository = mock(SessionRepository.class);

		FixedDurationExpirationSessionRepositoryBeanPostProcessor beanPostProcessor =
			new FixedDurationExpirationSessionRepositoryBeanPostProcessor(expirationTimeout);

		Object sessionRepository =
			beanPostProcessor.postProcessAfterInitialization(mockSessionRepository, "sessionRepository");

		assertThat(sessionRepository).isNotSameAs(mockSessionRepository);
		assertThat(sessionRepository).isInstanceOf(FixedDurationExpirationSessionRepository.class);
		assertThat(((FixedDurationExpirationSessionRepository<?>) sessionRepository).getExpirationTimeout()
			.orElse(null)).isEqualTo(expirationTimeout);
		assertThat(this.<SessionRepository<?>>invokeMethod(sessionRepository, "getDelegate"))
			.isEqualTo(mockSessionRepository);
	}
}
