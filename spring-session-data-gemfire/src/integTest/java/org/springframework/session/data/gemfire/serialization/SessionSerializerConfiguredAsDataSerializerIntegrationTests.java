/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.data.gemfire.serialization.data.support.DataSerializerSessionSerializerAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests to assert that any user-provided, custom {@link SessionSerializer} not bound to either
 * GemFire/Geode's Data Serialization of PDX Serialization framework is wrapped in
 * the {@link DataSerializerSessionSerializerAdapter} when the {@link DataSerializerSessionSerializerAdapter}
 * is present as a bean in the Spring context.
 *
 * @author John Blum
 * @see org.apache.geode.DataSerializer
 * @see ClientCache
 * @see ClientCacheApplication
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see DataSerializerSessionSerializerAdapter
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class SessionSerializerConfiguredAsDataSerializerIntegrationTests extends AbstractGemFireIntegrationTests {

	@Autowired
	private ClientCache gemfireCache;

	@Autowired
	@Qualifier(GemFireHttpSessionConfiguration.SESSION_SERIALIZER_BEAN_ALIAS)
	private SessionSerializer<?, ?, ?> configuredSessionSerializer;

	@Autowired
	@Qualifier("customSessionSerializer")
	private SessionSerializer<?, ?, ?> customSessionSerializer;

	@Autowired(required = false)
	@Qualifier("dataSerializerSessionSerializer")
	private SessionSerializer<?, ? , ?> dataSerializerSessionSerializer;

	@Test
	public void gemfireCachePdxSerializerIsNull() {
		assertThat(this.gemfireCache.getPdxSerializer()).isNull();
	}

	@Test
	public void dataSerializerSessionSerializerIsPresent() {
		assertThat(this.dataSerializerSessionSerializer).isInstanceOf(DataSerializerSessionSerializerAdapter.class);
	}

	@Test
	public void configuredSessionSerializerIsSetToCustomSessionSerializer() {
		assertThat(this.configuredSessionSerializer).isSameAs(this.customSessionSerializer);
	}

	@ClientCacheApplication(
		name = "SessionSerializerConfiguredAsDataSerializerIntegrationTests",
		logLevel = "error"
	)
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.LOCAL,
		poolName = "DEFAULT",
		sessionSerializerBeanName = "customSessionSerializer"
	)
	static class TestConfiguration {

		@Bean
		DataSerializerSessionSerializerAdapter<?> dataSerializerSessionSerializer() {
			return new DataSerializerSessionSerializerAdapter<>();
		}

		@Bean
		SessionSerializer<?, ?, ?> customSessionSerializer() {
			return mock(SessionSerializer.class);
		}
	}
}
