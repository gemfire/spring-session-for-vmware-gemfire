/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.config.annotation.web.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import java.util.Optional;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertySource;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.support.SpringSessionGemFireConfigurer;
import org.springframework.session.data.gemfire.expiration.SessionExpirationPolicy;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;

/**
 * Integration Tests testing {@link SpringSessionGemFireConfigurer} based configuration of either Apache Geode
 * or Pivotal GemFire as the (HTTP) {@link Session} state management provider in Spring Session.
 *
 * @author John Blum
 * @see Test
 * @see org.mockito.Mockito
 * @see ConfigurableApplicationContext
 * @see AnnotationConfigApplicationContext
 * @see Bean
 * @see Configuration
 * @see PropertySourcesPlaceholderConfigurer
 * @see PropertySource
 * @see ClientCacheApplication
 * @see EnableGemFireMockObjects
 * @see MockPropertySource
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see SpringSessionGemFireConfigurer
 * @see SessionExpirationPolicy
 * @see SessionSerializer
 * @since 2.0.4
 */
@SuppressWarnings("unused")
public class ConfigurerBasedGemFireHttpSessionConfigurationIntegrationTests extends AbstractGemFireIntegrationTests {

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
	public void onlySpringSessionGemFireConfigurerImplementedCallbacksOverrideAnnotationAttributesAndPropertyConfiguration() {

		MockPropertySource testPropertySource = new MockPropertySource("TestProperties")
			.withProperty("spring.session.data.gemfire.cache.client.pool.name", "Car")
			.withProperty("spring.session.data.gemfire.cache.client.region.shortcut", ClientRegionShortcut.LOCAL_PERSISTENT.name())
			.withProperty("spring.session.data.gemfire.session.attributes.indexable", "firstName, lastName")
			.withProperty("spring.session.data.gemfire.session.expiration.max-inactive-interval-seconds", "600")
			.withProperty("spring.session.data.gemfire.session.region.name", "PropertyRegionName")
			.withProperty("spring.session.data.gemfire.session.expiration.bean-name", "MockSessionExpirationPolicy");

		this.applicationContext = newApplicationContext(testPropertySource, TestConfiguration.class);

		GemFireHttpSessionConfiguration sessionConfiguration =
			this.applicationContext.getBean(GemFireHttpSessionConfiguration.class);

		assertThat(sessionConfiguration).isNotNull();
		assertThat(sessionConfiguration.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.CACHING_PROXY);
		assertThat(sessionConfiguration.getMaxInactiveIntervalInSeconds()).isEqualTo(3600);
		assertThat(sessionConfiguration.getPoolName()).isEqualTo("Dead");
		assertThat(sessionConfiguration.getSessionRegionName()).isEqualTo("ConfigurerRegionName");
		assertThat(sessionConfiguration.getSessionExpirationPolicyBeanName().orElse(null)).isEqualTo("ConfigurerSessionExpirationPolicy");
		assertThat(sessionConfiguration.getSessionSerializerBeanName()).isEqualTo("TestSessionSerializer");
	}

	@Test
	public void usesPrimarySpringSessionGemFireConfigurerWhenPresent() {

		MockPropertySource testPropertySource = new MockPropertySource("TestProperties")
			.withProperty("test.cache.client.pool.name", "Car")
			.withProperty("test.cache.client.region.shortcut", ClientRegionShortcut.CACHING_PROXY.name())
			.withProperty("test.session.expiration.max-inactive-interval-seconds", "300")
			.withProperty("test.session.region.name", "TestSessionRegionName")
			.withProperty("spring.session.data.gemfire.cache.client.region.shortcut", ClientRegionShortcut.LOCAL_PERSISTENT.name())
			.withProperty("spring.session.data.gemfire.session.expiration.max-inactive-interval-seconds", "120")
			.withProperty("spring.session.data.gemfire.session.region.name", "PropertyRegionName");

		this.applicationContext = newApplicationContext(testPropertySource,
			TestConfiguration.class, TestSpringSessionGemFireConfigurerConfiguration.class);

		GemFireHttpSessionConfiguration sessionConfiguration =
			this.applicationContext.getBean(GemFireHttpSessionConfiguration.class);

		assertThat(sessionConfiguration).isNotNull();
		assertThat(sessionConfiguration.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.CACHING_PROXY);
		assertThat(sessionConfiguration.getMaxInactiveIntervalInSeconds()).isEqualTo(300);
		assertThat(sessionConfiguration.getPoolName()).isEqualTo("Car");
		assertThat(sessionConfiguration.getSessionRegionName()).isEqualTo("TestSessionRegionName");
		assertThat(sessionConfiguration.getSessionExpirationPolicyBeanName().orElse(null)).isEqualTo("TestSessionExpirationPolicy");
		assertThat(sessionConfiguration.getSessionSerializerBeanName()).isEqualTo("TestSessionSerializer");
	}

	@Test
	public void usesSpringSessionGemFireConfigurerWhenPresent() {

		this.applicationContext = newApplicationContext(TestConfiguration.class);

		GemFireHttpSessionConfiguration sessionConfiguration =
			this.applicationContext.getBean(GemFireHttpSessionConfiguration.class);

		assertThat(sessionConfiguration).isNotNull();
		assertThat(sessionConfiguration.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.CACHING_PROXY);
		assertThat(sessionConfiguration.getMaxInactiveIntervalInSeconds()).isEqualTo(3600);
		assertThat(sessionConfiguration.getPoolName()).isEqualTo("Dead");
		assertThat(sessionConfiguration.getSessionRegionName()).isEqualTo("ConfigurerRegionName");
		assertThat(sessionConfiguration.getSessionExpirationPolicyBeanName().orElse(null)).isEqualTo("ConfigurerSessionExpirationPolicy");
		assertThat(sessionConfiguration.getSessionSerializerBeanName()).isEqualTo("TestSessionSerializer");
	}

	@ClientCacheApplication
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.LOCAL,
		maxInactiveIntervalInSeconds = 900,
		poolName = "Swimming",
		regionName = "AnnotationAttributeRegionName",
		sessionExpirationPolicyBeanName = "TestSessionExpirationPolicy",
		sessionSerializerBeanName = "TestSessionSerializer"
	)
	@EnableGemFireMockObjects(destroyOnEvents = ContextClosedEvent.class)
	static class TestConfiguration {

		@Bean("Car")
		Pool mockCarPool() {
			return mock(Pool.class, "Car");
		}

		@Bean("Dead")
		Pool mockDeadPool() {
			return mock(Pool.class, "Dead");
		}

		@Bean("Swimming")
		Pool mockSwimmingPool() {
			return mock(Pool.class, "Swimming");
		}

		@Bean("TestSessionExpirationPolicy")
		SessionExpirationPolicy testSessionExpirationPolicy() {
			return mock(SessionExpirationPolicy.class);
		}

		@Bean("TestSessionSerializer")
		Object testSessionSerializer() {
			return mock(SessionSerializer.class);
		}

		@Bean
		SpringSessionGemFireConfigurer testSpringSessionGemFireConfigurer() {

			return new SpringSessionGemFireConfigurer() {

				@Override
				public ClientRegionShortcut getClientRegionShortcut() {
					return ClientRegionShortcut.CACHING_PROXY;
				}

				@Override
				public int getMaxInactiveIntervalInSeconds() {
					return 3600;
				}

				@Override
				public String getPoolName() {
					return "Dead";
				}

				@Override
				public String getRegionName() {
					return "ConfigurerRegionName";
				}

				@Override
				public String getSessionExpirationPolicyBeanName() {
					return "ConfigurerSessionExpirationPolicy";
				}
			};
		}
	}

	@Configuration
	static class TestSpringSessionGemFireConfigurerConfiguration {

		//@Value("${test.cache.client.region.shortcut:PROXY}")
		//private ClientRegionShortcut clientRegionShortcut;

		//@Value("${test.session.expiration.max-inactive-interval-seconds:600}")
		//private int maxInactiveIntervalInSeconds;

		@Bean
		static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		@Primary
		SpringSessionGemFireConfigurer primarySpringSessionGemFireConfigurer(
				@Value("${test.cache.client.pool.name:geodePool}") String poolName,
				@Value("${test.cache.client.region.shortcut:LOCAL}") ClientRegionShortcut clientRegionShortcut,
				@Value("${test.session.expiration.max-inactive-interval-seconds:600}") int maxInactiveIntervalInSeconds,
				@Value("${test.session.region.name:MockSessionRegionName}") String regionName) {

			return new SpringSessionGemFireConfigurer() {

				@Override
				public ClientRegionShortcut getClientRegionShortcut() {
					return clientRegionShortcut;
				}

				@Override
				public int getMaxInactiveIntervalInSeconds() {
					return maxInactiveIntervalInSeconds;
				}

				@Override
				public String getPoolName() {
					return poolName;
				}

				@Override
				public String getRegionName() {
					return regionName;
				}
			};
		}
	}
}
