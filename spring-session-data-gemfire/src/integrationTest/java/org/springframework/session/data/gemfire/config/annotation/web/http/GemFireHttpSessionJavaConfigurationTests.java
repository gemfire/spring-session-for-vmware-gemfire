/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.config.annotation.web.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.query.Index;
import org.apache.geode.cache.query.QueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.support.GemFireUtils;
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

	protected <K, V> Region<K, V> assertCacheAndRegion(Cache gemfireCache,
			String regionName, DataPolicy dataPolicy) {

		Region<K, V> region = gemfireCache.getRegion(regionName);

		assertRegion(region, regionName, dataPolicy);

		return region;
	}

	@Test
	public void gemfireCacheConfigurationIsValid() {

		Region<Object, Session> example =
			assertCacheAndRegion(this.gemfireCache, "JavaExample", DataPolicy.REPLICATE);

		assertEntryIdleTimeout(example, ExpirationAction.INVALIDATE, 900);
	}

	@Test
	public void verifyGemFireExampleCacheRegionPrincipalNameIndexWasCreatedSuccessfully() {

		Region<Object, Session> example =
			assertCacheAndRegion(this.gemfireCache, "JavaExample", DataPolicy.REPLICATE);

		QueryService queryService = example.getRegionService().getQueryService();

		assertThat(queryService).isNotNull();

		Index principalNameIndex = queryService.getIndex(example, "principalNameIndex");

		assertIndex(principalNameIndex, "principalName", example.getFullPath());
	}

	@Test
	public void verifyGemFireExampleCacheRegionSessionAttributesIndexWasNotCreated() {

		Region<Object, Session> example =
			assertCacheAndRegion(this.gemfireCache, "JavaExample", DataPolicy.REPLICATE);

		QueryService queryService = example.getRegionService().getQueryService();

		assertThat(queryService).isNotNull();

		Index sessionAttributesIndex = queryService.getIndex(example, "sessionAttributesIndex");

		assertThat(sessionAttributesIndex).isNull();
	}

	@ClientCacheApplication(
		name = "GemFireHttpSessionJavaConfigurationTests",
		logLevel = "error"
	)
	@EnableGemFireHttpSession(
		maxInactiveIntervalInSeconds = 900,
		regionName = "JavaExample",
		serverRegionShortcut = RegionShortcut.REPLICATE
	)
	static class GemFireConfiguration { }

}
