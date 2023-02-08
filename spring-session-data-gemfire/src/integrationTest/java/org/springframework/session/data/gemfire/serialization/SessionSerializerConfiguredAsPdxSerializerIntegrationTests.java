/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.data.gemfire.serialization.pdx.support.PdxSerializerSessionSerializerAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests to assert that any user-provided, custom {@link SessionSerializer} not bound to either
 * GemFire/Geode's Data Serialization of PDX Serialization framework is wrapped in
 * the {@link PdxSerializerSessionSerializerAdapter}.
 *
 * @author John Blum
 * @see GemFireCache
 * @see org.apache.geode.pdx.PdxSerializer
 * @see Bean
 * @see ClientCacheApplication
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see PdxSerializerSessionSerializerAdapter
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class SessionSerializerConfiguredAsPdxSerializerIntegrationTests extends AbstractGemFireIntegrationTests {

	@Autowired
	private GemFireCache gemfireCache;

	@Autowired
	@Qualifier(GemFireHttpSessionConfiguration.SESSION_SERIALIZER_BEAN_ALIAS)
	private SessionSerializer<?, ?, ?> configuredSessionSerializer;

	@Autowired
	@Qualifier("customSessionSerializer")
	private SessionSerializer<?, ?, ?> customSessionSerializer;

	@Test
	public void gemfireCachePdxSerializerIsPdxSerializerSessionSerializerAdapter() {

		assertThat(this.gemfireCache.getPdxSerializer()).isInstanceOf(PdxSerializerSessionSerializerAdapter.class);

		assertThat(((PdxSerializerSessionSerializerAdapter<?>) this.gemfireCache.getPdxSerializer()).getSessionSerializer())
			.isSameAs(this.customSessionSerializer);
	}

	@Test
	public void configuredSessionSerializerIsSetToCustomSessionSerializer() {
		assertThat(this.configuredSessionSerializer).isSameAs(this.customSessionSerializer);
	}

	@ClientCacheApplication(
		name = "SessionSerializerConfiguredAsPdxSerializerIntegrationTests",
		logLevel = "error"
	)
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.LOCAL,
		poolName = "DEFAULT",
		sessionSerializerBeanName = "customSessionSerializer"
	)
	static class TestConfiguration {

		@Bean
		SessionSerializer<?, ?, ?> customSessionSerializer() {
			return mock(SessionSerializer.class);
		}
	}
}
