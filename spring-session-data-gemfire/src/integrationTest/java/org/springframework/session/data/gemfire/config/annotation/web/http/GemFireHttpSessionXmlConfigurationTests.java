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
import org.apache.geode.cache.query.Index;
import org.apache.geode.cache.query.QueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.support.GemFireUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Test suite of test cases testing the configuration of Spring Session backed by GemFire
 * using XML configuration meta-data.
 *
 * @author John Blum
 * @since 1.1.0
 * @see Test
 * @see Cache
 * @see Region
 * @see Index
 * @see QueryService
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see DirtiesContext
 * @see ContextConfiguration
 * @see SpringRunner
 * @see WebAppConfiguration
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@WebAppConfiguration
public class GemFireHttpSessionXmlConfigurationTests extends AbstractGemFireIntegrationTests {

	@Autowired
	@SuppressWarnings("all")
	private Cache gemfireCache;

	protected <K, V> Region<K, V> assertCacheAndRegion(Cache gemfireCache, String regionName, DataPolicy dataPolicy) {

		assertThat(GemFireUtils.isPeer(gemfireCache)).isTrue();

		Region<K, V> region = gemfireCache.getRegion(regionName);

		assertRegion(region, regionName, dataPolicy);

		return region;
	}

	@Test
	public void gemfireCacheConfigurationIsValid() {

		Region<Object, Session> example =
			assertCacheAndRegion(this.gemfireCache, "XmlExample", DataPolicy.NORMAL);

		assertEntryIdleTimeout(example, ExpirationAction.INVALIDATE, 3600);
	}

	@Test
	public void verifyGemFireExampleCacheRegionPrincipalNameIndexWasCreatedSuccessfully() {

		Region<Object, Session> example =
			assertCacheAndRegion(this.gemfireCache, "XmlExample", DataPolicy.NORMAL);

		QueryService queryService = example.getRegionService().getQueryService();

		assertThat(queryService).isNotNull();

		Index principalNameIndex = queryService.getIndex(example, "principalNameIndex");

		assertIndex(principalNameIndex, "principalName", example.getFullPath());
	}

	@Test
	public void verifyGemFireExampleCacheRegionSessionAttributesIndexWasCreatedSuccessfully() {

		Region<Object, Session> example =
			assertCacheAndRegion(this.gemfireCache, "XmlExample", DataPolicy.NORMAL);

		QueryService queryService = example.getRegionService().getQueryService();

		assertThat(queryService).isNotNull();

		Index sessionAttributesIndex = queryService.getIndex(example, "sessionAttributesIndex");

		assertIndex(sessionAttributesIndex, "s.attributes['one', 'two', 'three']",
			String.format("%1$s s", example.getFullPath()));
	}
}
