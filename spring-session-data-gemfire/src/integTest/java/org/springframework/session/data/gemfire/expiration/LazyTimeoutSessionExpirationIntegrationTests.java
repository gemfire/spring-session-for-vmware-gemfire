/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.expiration;

import static org.assertj.core.api.Assertions.assertThat;
import com.vmware.gemfire.testcontainers.GemFireCluster;
import java.io.IOException;
import java.time.Duration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.expiration.config.FixedDurationExpirationSessionRepositoryBeanPostProcessor;
import org.springframework.session.data.gemfire.expiration.repository.FixedDurationExpirationSessionRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests asserting lazy expiration timeouts on {@link Session} access
 * using {@link FixedDurationExpirationSessionRepository}.
 *
 * @author John Blum
 * @see Test
 * @see Bean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see EnableGemFireMockObjects
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see FixedDurationExpirationSessionRepositoryBeanPostProcessor
 * @see FixedDurationExpirationSessionRepository
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class LazyTimeoutSessionExpirationIntegrationTests extends AbstractGemFireIntegrationTests {

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1);
		gemFireCluster.acceptLicense().start();
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}
	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void sessionRepositoryIsAFixedDurationExpirationSessionRepository() {

		assertThat(this.<Session, SessionRepository>getSessionRepository())
			.isInstanceOf(FixedDurationExpirationSessionRepository.class);
	}

	@Test
	public void sessionsExpiresAfterFixedDurationOnLazyAccess() {

		Session session = save(touch(createSession()));

		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isExpired()).isFalse();

		waitOn(() -> false, Duration.ofSeconds(1).toMillis());

		Session loadedSession = get(session.getId());

		assertThat(loadedSession).isEqualTo(session);
		assertThat(loadedSession.isExpired()).isFalse();

		waitOn(() -> false, Duration.ofSeconds(1).toMillis() + 1);

		Session expiredSession = get(loadedSession.getId());

		assertThat(expiredSession).isNull();
	}

	@ClientCacheApplication
	@EnableGemFireHttpSession
	@EnableGemFireMockObjects
	static class TestConfiguration {

		@Bean
		PoolFactoryBean gemfirePool() {
			PoolFactoryBean poolFactory = new PoolFactoryBean();
			poolFactory.addServers(new ConnectionEndpoint("localhost", gemFireCluster.getLocatorPort()));
			return poolFactory;
		}

		@Bean
		FixedDurationExpirationSessionRepositoryBeanPostProcessor fixedDurationExpirationBeanPostProcessor() {
			return new FixedDurationExpirationSessionRepositoryBeanPostProcessor(Duration.ofSeconds(2));
		}
	}
}
