/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.config.annotation.web.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertySource;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;

/**
 * Integration Tests testing property-based configuration of either Apache Geode or Pivotal GemFire
 * as the (HTTP) {@link Session} state management provider in Spring Session.
 *
 * @author John Blum
 * @see Test
 * @see ConfigurableApplicationContext
 * @see AnnotationConfigApplicationContext
 * @see PropertySource
 * @see ClientCacheApplication
 * @see EnableGemFireMockObjects
 * @see MockPropertySource
 * @see AbstractGemFireIntegrationTests
 * @since 2.0.4
 */
@SuppressWarnings("unused")
public class PropertyBasedGemFireHttpSessionConfigurationIntegrationTests extends AbstractGemFireIntegrationTests {

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		Optional.ofNullable(this.applicationContext).ifPresent(ConfigurableApplicationContext::close);
	}

	private ConfigurableApplicationContext newApplicationContext(Class<?>... annotatedClasses) {
		return newApplicationContext(new MockPropertySource("TestProperties"), annotatedClasses);
	}

	private ConfigurableApplicationContext newApplicationContext(PropertySource<?> testPropertySource,
			Class<?>... annotatedClasses) {

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		applicationContext.getEnvironment().getPropertySources().addFirst(testPropertySource);
		applicationContext.register(annotatedClasses);
		applicationContext.registerShutdownHook();
		applicationContext.refresh();

		return applicationContext;
	}

	@Test
	public void usesAnnotationAttributeConfigurationExclusively() {

		this.applicationContext = newApplicationContext(TestConfiguration.class);

		GemFireHttpSessionConfiguration sessionConfiguration =
			this.applicationContext.getBean(GemFireHttpSessionConfiguration.class);

		assertThat(sessionConfiguration).isNotNull();
		assertThat(sessionConfiguration.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.LOCAL);
		assertThat(sessionConfiguration.getIndexableSessionAttributes()).containsExactly("one", "two");
		assertThat(sessionConfiguration.getMaxInactiveIntervalInSeconds()).isEqualTo(900);
		assertThat(sessionConfiguration.getPoolName()).isEqualTo("Swimming");
		assertThat(sessionConfiguration.getServerRegionShortcut()).isEqualTo(RegionShortcut.LOCAL);
		assertThat(sessionConfiguration.getSessionExpirationPolicyBeanName().orElse(null)).isEqualTo("TestSessionExpirationPolicy");
		assertThat(sessionConfiguration.getSessionRegionName()).isEqualTo("AnnotationAttributeRegionName");
		assertThat(sessionConfiguration.getSessionSerializerBeanName()).isEqualTo("TestSessionSerializer");
	}

	@Test
	public void usesAnnotationAttributesAndPropertyBasedConfiguration() {

		MockPropertySource testPropertySource = new MockPropertySource("TestProperties")
			.withProperty("spring.session.data.gemfire.cache.client.pool.name", "Dead")
			.withProperty("spring.session.data.gemfire.session.attributes.indexable", "two, four")
			.withProperty("spring.session.data.gemfire.session.expiration.bean-name", "MockSessionExpirationPolicy")
			.withProperty("spring.session.data.gemfire.session.region.name", "TestRegionName");

		this.applicationContext = newApplicationContext(testPropertySource, TestConfiguration.class);

		GemFireHttpSessionConfiguration sessionConfiguration =
			this.applicationContext.getBean(GemFireHttpSessionConfiguration.class);

		assertThat(sessionConfiguration).isNotNull();
		assertThat(sessionConfiguration.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.LOCAL);
		assertThat(sessionConfiguration.getIndexableSessionAttributes()).containsExactly("two", "four");
		assertThat(sessionConfiguration.getMaxInactiveIntervalInSeconds()).isEqualTo(900);
		assertThat(sessionConfiguration.getPoolName()).isEqualTo("Dead");
		assertThat(sessionConfiguration.getServerRegionShortcut()).isEqualTo(RegionShortcut.LOCAL);
		assertThat(sessionConfiguration.getSessionExpirationPolicyBeanName().orElse(null)).isEqualTo("MockSessionExpirationPolicy");
		assertThat(sessionConfiguration.getSessionRegionName()).isEqualTo("TestRegionName");
		assertThat(sessionConfiguration.getSessionSerializerBeanName()).isEqualTo("TestSessionSerializer");
	}

	@Test
	public void usesPropertyBasedConfigurationExclusively() {

		MockPropertySource testPropertySource = new MockPropertySource("TestProperties")
			.withProperty("spring.session.data.gemfire.cache.client.pool.name", "Dead")
			.withProperty("spring.session.data.gemfire.cache.client.region.shortcut", ClientRegionShortcut.CACHING_PROXY.name())
			.withProperty("spring.session.data.gemfire.cache.server.region.shortcut", RegionShortcut.REPLICATE_PERSISTENT_OVERFLOW.name())
			.withProperty("spring.session.data.gemfire.session.attributes.indexable", "firstName, lastName")
			.withProperty("spring.session.data.gemfire.session.expiration.bean-name", "MockSessionExpirationPolicy")
			.withProperty("spring.session.data.gemfire.session.expiration.max-inactive-interval-seconds", "3600")
			.withProperty("spring.session.data.gemfire.session.region.name", "PropertyRegionName")
			.withProperty("spring.session.data.gemfire.session.serializer.bean-name", "MockSessionSerializer");

		this.applicationContext = newApplicationContext(testPropertySource, TestConfiguration.class);

		GemFireHttpSessionConfiguration sessionConfiguration =
			this.applicationContext.getBean(GemFireHttpSessionConfiguration.class);

		assertThat(sessionConfiguration).isNotNull();
		assertThat(sessionConfiguration.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.CACHING_PROXY);
		assertThat(sessionConfiguration.getIndexableSessionAttributes()).containsExactly("firstName", "lastName");
		assertThat(sessionConfiguration.getMaxInactiveIntervalInSeconds()).isEqualTo(3600);
		assertThat(sessionConfiguration.getPoolName()).isEqualTo("Dead");
		assertThat(sessionConfiguration.getServerRegionShortcut()).isEqualTo(RegionShortcut.REPLICATE_PERSISTENT_OVERFLOW);
		assertThat(sessionConfiguration.getSessionExpirationPolicyBeanName().orElse(null)).isEqualTo("MockSessionExpirationPolicy");
		assertThat(sessionConfiguration.getSessionRegionName()).isEqualTo("PropertyRegionName");
		assertThat(sessionConfiguration.getSessionSerializerBeanName()).isEqualTo("MockSessionSerializer");
	}

	@ClientCacheApplication
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.LOCAL,
		indexableSessionAttributes = { "one", "two" },
		maxInactiveIntervalInSeconds = 900,
		poolName = "Swimming",
		regionName = "AnnotationAttributeRegionName",
		serverRegionShortcut = RegionShortcut.LOCAL,
		sessionExpirationPolicyBeanName = "TestSessionExpirationPolicy",
		sessionSerializerBeanName = "TestSessionSerializer"
	)
	@EnableGemFireMockObjects
	static class TestConfiguration {

		@Bean("Dead")
		Pool mockDeadPool() {
			return mock(Pool.class);
		}

		@Bean("Swimming")
		Pool mockSwimmingPool() {
			return mock(Pool.class);
		}

		@Bean("MockSessionSerializer")
		Object mockSessionSerializer() {
			return mock(SessionSerializer.class);
		}

		@Bean("TestSessionExpirationPolicy")
		SessionExpirationPolicy testSessionExpirationPolicy() {
			return mock(SessionExpirationPolicy.class);
		}

		@Bean("TestSessionSerializer")
		Object testSessionSerializer() {
			return mock(SessionSerializer.class);
		}
	}
}
