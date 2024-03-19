/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the concurrent access of a {@link Session} stored in an Apache Geode
 * {@link ClientCache client} {@link ClientRegionShortcut#PROXY} {@link Region}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see Region
 * @see ClientCache
 * @see AnnotationConfigApplicationContext
 * @see CacheServerApplication
 * @see ClientCacheApplication
 * @see Session
 * @see AbstractConcurrentSessionOperationsIntegrationTests
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.x
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = ConcurrentSessionOperationsUsingClientProxyRegionIntegrationTests.GemFireClientConfiguration.class
)
public class ConcurrentSessionOperationsUsingClientProxyRegionIntegrationTests
		extends AbstractConcurrentSessionOperationsIntegrationTests {

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		startGemFireServer(GemFireServerConfiguration.class);
	}

	@ClientCacheApplication(subscriptionEnabled = true)
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.PROXY,
		poolName = "DEFAULT",
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	static class GemFireClientConfiguration { }

	@CacheServerApplication(name = "ConcurrentSessionOperationsUsingClientProxyRegionIntegrationTests")
	@EnableGemFireHttpSession(
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	static class GemFireServerConfiguration {

		public static void main(String[] args) {

			AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(GemFireServerConfiguration.class);

			applicationContext.registerShutdownHook();
		}
	}
}
