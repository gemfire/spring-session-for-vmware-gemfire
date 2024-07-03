/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.config.annotation.web.http;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import java.io.IOException;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.Region;
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
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Integration test to test the configuration of Spring Session backed by GemFire
 * using Java-based configuration meta-data.
 *
 * @author John Blum
 * @since 1.1.0
 * @see Test
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see DirtiesContext
 * @see ContextConfiguration
 * @see SpringJUnit4ClassRunner
 * @see WebAppConfiguration
 * @see Cache
 * @see Region
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@WebAppConfiguration
public class GemFireHttpSessionJavaConfigurationTests extends AbstractGemFireIntegrationTests {

	@Autowired @SuppressWarnings("unused")
	private Cache gemfireCache;

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

	protected <K, V> Region<K, V> assertCacheAndRegion(Cache gemfireCache,
			String regionName, DataPolicy dataPolicy) {

		Region<K, V> region = gemfireCache.getRegion(regionName);

		assertRegion(region, regionName, dataPolicy);

		return region;
	}

	@Test
	public void gemfireCacheConfigurationIsValid() {

		Region<Object, Session> example =
			assertCacheAndRegion(this.gemfireCache, "JavaExample", DataPolicy.NORMAL);

		assertEntryIdleTimeout(example, ExpirationAction.INVALIDATE, 900);
	}

	@ClientCacheApplication(
		name = "GemFireHttpSessionJavaConfigurationTests",
		logLevel = "error"
	)
	@EnableGemFireHttpSession(
		maxInactiveIntervalInSeconds = 900,
		regionName = "JavaExample",
		clientRegionShortcut = ClientRegionShortcut.LOCAL
	)
	static class GemFireConfiguration {
		@Bean
		PoolFactoryBean gemfirePool() {
			PoolFactoryBean poolFactory = new PoolFactoryBean();
			poolFactory.addServers(new ConnectionEndpoint("localhost", gemFireCluster.getLocatorPort()));
			return poolFactory;
		}
	}
}
