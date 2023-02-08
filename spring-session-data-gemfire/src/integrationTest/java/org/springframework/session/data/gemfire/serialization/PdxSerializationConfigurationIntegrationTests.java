/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests to assert that GemFire/Geode's PDX Serialization framework is configured and applied properly.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class PdxSerializationConfigurationIntegrationTests extends AbstractGemFireIntegrationTests {

	@Autowired
	@Qualifier(GemFireHttpSessionConfiguration.SESSION_SERIALIZER_BEAN_ALIAS)
	private SessionSerializer<?, ?, ?> configuredSessionSerializer;

	@Autowired
	@Qualifier(GemFireHttpSessionConfiguration.SESSION_PDX_SERIALIZER_BEAN_NAME)
	private SessionSerializer<?, ?, ?> pdxSerializableSessionSerialzer;

	@Autowired
	private GemFireCache gemfireCache;

	@Test
	public void gemfireCachePdxSerializerIsSetToPdxSerializableSessionSerializer() {
		assertThat(this.gemfireCache.getPdxSerializer()).isSameAs(this.pdxSerializableSessionSerialzer);
	}

	@Test
	public void configuredSessionSerializerIsSetToPdxSerializableSessionSerializer() {
		assertThat(this.configuredSessionSerializer).isSameAs(this.pdxSerializableSessionSerialzer);

	}

	@ClientCacheApplication(
		name = "PdxSerializationConfigurationIntegrationTests",
		logLevel = "error"
	)
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.LOCAL,
		poolName = "DEFAULT",
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_PDX_SERIALIZER_BEAN_NAME
	)
	@SuppressWarnings("all")
	static class TestConfiguration { }

}
