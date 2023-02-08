/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.gemfire.AbstractGemFireUnitTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Unit Tests testing the configuration of a custom {@link CookieSerializer} in the context of SSDG.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.session.data.gemfire.AbstractGemFireUnitTests
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession
 * @see org.springframework.session.web.http.CookieHttpSessionIdResolver
 * @see org.springframework.session.web.http.CookieSerializer
 * @see org.springframework.session.web.http.HttpSessionIdResolver
 * @see org.springframework.session.web.http.SessionRepositoryFilter
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class CustomCookieSerializerConfigurationUnitTests extends AbstractGemFireUnitTests {

	@Autowired
	private CookieSerializer cookieSerializer;

	@Autowired
	private SessionRepositoryFilter<?> filter;

	@Test
	public void customCookieSerializerConfigured() {

		assertThat(this.filter).isNotNull();
		assertThat(this.cookieSerializer).isInstanceOf(TestCookieSerializer.class);

		HttpSessionIdResolver configuredHttpSessionIdResolver =
			getValue(this.filter, "httpSessionIdResolver");

		assertThat(configuredHttpSessionIdResolver).isInstanceOf(CookieHttpSessionIdResolver.class);

		CookieSerializer configuredCookieSerializer =
			getValue(configuredHttpSessionIdResolver, "cookieSerializer");

		assertThat(configuredCookieSerializer).isSameAs(this.cookieSerializer);
	}

	@Configuration
	@EnableGemFireHttpSession
	static class TestConfiguration extends BaseTestConfiguration {

		@Bean
		CookieSerializer mockCookieSerializer() {
			return mock(TestCookieSerializer.class);
		}
	}

	interface TestCookieSerializer extends CookieSerializer { }

}
