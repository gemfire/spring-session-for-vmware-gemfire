/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.expiration;

import static org.assertj.core.api.Assertions.assertThat;
import com.vmware.gemfire.testcontainers.GemFireCluster;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.data.gemfire.expiration.config.SessionExpirationTimeoutAware;
import org.springframework.session.data.gemfire.expiration.support.SessionExpirationPolicyCustomExpiryAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

/**
 * Integration Tests for {@link EnableGemFireHttpSession} and {@link GemFireHttpSessionConfiguration}
 * involving {@link Session} expiration configuration.
 *
 * @author John Blum
 * @see Test
 * @see ExpirationAction
 * @see ExpirationAttributes
 * @see Region
 * @see RegionAttributes
 * @see Bean
 * @see EnableGemFireMockObjects
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see SessionExpirationTimeoutAware
 * @see SessionExpirationPolicyCustomExpiryAdapter
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class CustomSessionExpirationConfigurationIntegrationTests extends AbstractGemFireIntegrationTests {

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SESSION_REGION_NAME)
	private Region<String, Object> sessions;

	@Autowired
	private SessionExpirationPolicy sessionExpirationPolicy;

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

	@SuppressWarnings("unchecked")
	private <T> T invokeMethod(Object target, String methodName) throws NoSuchMethodException {

		Method method = target.getClass().getDeclaredMethod(methodName);

		ReflectionUtils.makeAccessible(method);

		return (T) ReflectionUtils.invokeMethod(method, target);
	}

	@Test
	public void sessionExpirationPolicyConfigurationIsCorrect() {

		assertThat(this.sessionExpirationPolicy).isInstanceOf(TestSessionExpirationPolicy.class);
		assertThat(this.sessionExpirationPolicy.determineExpirationTimeout(null).orElse(null))
			.isEqualTo(Duration.ofMinutes(10));
	}

	@Test
	public void regionCustomEntryIdleTimeoutExpirationConfigurationIsCorrect() throws Exception {

		assertThat(this.sessions).isNotNull();

		RegionAttributes<String, Object> sessionRegionAttributes = this.sessions.getAttributes();

		assertThat(sessionRegionAttributes).isNotNull();
		assertThat(sessionRegionAttributes.getCustomEntryIdleTimeout())
			.isInstanceOf(SessionExpirationPolicyCustomExpiryAdapter.class);

		SessionExpirationPolicyCustomExpiryAdapter customEntryIdleTimeout =
			(SessionExpirationPolicyCustomExpiryAdapter) sessionRegionAttributes.getCustomEntryIdleTimeout();

		SessionExpirationPolicy actualSessionExpirationPolicy =
			invokeMethod(customEntryIdleTimeout, "getSessionExpirationPolicy");

		assertThat(actualSessionExpirationPolicy).isEqualTo(this.sessionExpirationPolicy);

	}

	@Test
	public void regionEntryIdleTimeoutExpirationConfigurationIsCorrect() {

		assertThat(this.sessions).isNotNull();

		RegionAttributes<String, Object> sessionRegionAttributes = this.sessions.getAttributes();

		assertThat(sessionRegionAttributes).isNotNull();

		ExpirationAttributes entryIdleTimeout = sessionRegionAttributes.getEntryIdleTimeout();

		assertThat(entryIdleTimeout).isNotNull();
		assertThat(entryIdleTimeout.getTimeout()).isEqualTo(600);
		assertThat(entryIdleTimeout.getAction()).isEqualTo(ExpirationAction.INVALIDATE);
	}

	@ClientCacheApplication
	@EnableGemFireMockObjects
	@EnableGemFireHttpSession(
		maxInactiveIntervalInSeconds = 600,
		sessionExpirationPolicyBeanName = "testSessionExpirationPolicy",
			clientRegionShortcut = ClientRegionShortcut.LOCAL
	)
	static class TestConfiguration {

		@Bean
		SessionExpirationPolicy testSessionExpirationPolicy() {
			return new TestSessionExpirationPolicy();
		}

		@Bean
		PoolFactoryBean gemfirePool() {
			PoolFactoryBean poolFactory = new PoolFactoryBean();
			poolFactory.addServers(new ConnectionEndpoint("localhost", gemFireCluster.getLocatorPort()));
			return poolFactory;
		}
	}

	static class TestSessionExpirationPolicy implements SessionExpirationPolicy, SessionExpirationTimeoutAware {

		private Duration expirationTimeout;

		@Override
		public void setExpirationTimeout(Duration expirationTimeout) {
			this.expirationTimeout = expirationTimeout;
		}

		@Override
		public Optional<Duration> determineExpirationTimeout(Session session) {
			return Optional.ofNullable(this.expirationTimeout);
		}
	}
}
