/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.PdxSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.data.gemfire.serialization.pdx.support.ComposablePdxSerializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests to assert that any user-configured {@link PdxSerializer} is composed with
 * Spring Session Data GemFire's Session-based {@link PdxSerializer}.
 *
 * @author John Blum
 * @see Test
 * @see ClientCache
 * @see ClientCacheApplication
 * @see ClientCacheConfigurer
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see ComposablePdxSerializer
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class CompositePdxSerializerConfigurationIntegrationTests extends AbstractGemFireIntegrationTests {

	@Autowired
	private ClientCache gemfireCache;

	@Autowired
	@Qualifier("mockPdxSerializer")
	private PdxSerializer mockPdxSerializer;

	@Autowired
	@Qualifier(GemFireHttpSessionConfiguration.SESSION_SERIALIZER_BEAN_ALIAS)
	private SessionSerializer configuredSessionSerializer;

	@Autowired
	@Qualifier(GemFireHttpSessionConfiguration.SESSION_PDX_SERIALIZER_BEAN_NAME)
	private SessionSerializer pdxSerializableSessionSerializer;

	@Test
	public void gemfireCachePdxSerializerIsACompositePdxSerializer() {

		PdxSerializer pdxSerializer = this.gemfireCache.getPdxSerializer();

		assertThat(pdxSerializer).isInstanceOf(ComposablePdxSerializer.class);
		assertThat(((ComposablePdxSerializer) pdxSerializer))
			.containsExactly((PdxSerializer) this.pdxSerializableSessionSerializer, this.mockPdxSerializer);
	}

	@Test
	public void configuredSessionSerializerIsSetToPdxSerializableSessionSerializer() {
		assertThat(this.configuredSessionSerializer).isSameAs(this.pdxSerializableSessionSerializer);
	}

	@ClientCacheApplication(
		name = "CompositePdxSerializerConfigurationIntegrationTests",
		logLevel = "error"
	)
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.LOCAL,
		poolName = "DEFAULT"
	)
	@SuppressWarnings("all")
	static class TestConfiguration {

		@Bean
		ClientCacheConfigurer clientCachePdxSerializerConfigurer(@Qualifier("mockPdxSerializer") PdxSerializer pdxSerializer) {
			return (beanName, clientCacheFactoryBean) -> clientCacheFactoryBean.setPdxSerializer(pdxSerializer);
		}

		@Bean
		PdxSerializer mockPdxSerializer() {
			return mock(PdxSerializer.class);
		}
	}
}
