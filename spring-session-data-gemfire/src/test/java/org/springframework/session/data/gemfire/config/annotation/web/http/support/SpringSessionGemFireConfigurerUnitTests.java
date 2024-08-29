/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.config.annotation.web.http.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.Test;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

/**
 * Unit Tests for {@link SpringSessionGemFireConfigurer}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.support.SpringSessionGemFireConfigurer
 * @since 2.1.1
 */
public class SpringSessionGemFireConfigurerUnitTests {

	private SpringSessionGemFireConfigurer newTestConfigurerWithAllOverrides() {

		return new SpringSessionGemFireConfigurer() {

			@Override
			public ClientRegionShortcut getClientRegionShortcut() {
				return ClientRegionShortcut.LOCAL;
			}

			@Override
			public int getMaxInactiveIntervalInSeconds() {
				return 300;
			}

			@Override
			public String getPoolName() {
				return "MockPool";
			}

			@Override
			public String getRegionName() {
				return "MockRegion";
			}

			@Override
			public String getSessionExpirationPolicyBeanName() {
				return "MockExpirationPolicy";
			}

			@Override
			public String getSessionSerializerBeanName() {
				return "MockSerializer";
			}
		};
	}

	private SpringSessionGemFireConfigurer newTestConfigurerWithNoOverrides() {
		return new SpringSessionGemFireConfigurer() { };
	}

	private SpringSessionGemFireConfigurer newTestConfigurerWithSelectOverrides() {

		return new SpringSessionGemFireConfigurer() {

			@Override
			public ClientRegionShortcut getClientRegionShortcut() {
				return ClientRegionShortcut.CACHING_PROXY;
			}

			@Override
			public int getMaxInactiveIntervalInSeconds() {
				return 600;
			}

			@Override
			public String getPoolName() {
				return "TestPool";
			}

			@Override
			public String getRegionName() {
				return "TestRegion";
			}
		};
	}

	private Method[] filterDeclaredMethods(Method[] methods) {

		List<String> targetMethodNames =
			Arrays.stream(nullSafeArray(SpringSessionGemFireConfigurer.class.getMethods(), Method.class))
				.map(Method::getName)
				.collect(Collectors.toList());

		return Arrays.stream(nullSafeArray(methods, Method.class))
			.filter(method -> targetMethodNames.contains(method.getName()))
			.toArray(Method[]::new);
	}

	@Test
	public void classGetDeclaredMethodsOnCustomConfigurerObjectHasAllMethods() {

		SpringSessionGemFireConfigurer testConfigurer = newTestConfigurerWithAllOverrides();

		assertThat(testConfigurer).isNotNull();
		assertThat(testConfigurer.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.LOCAL);
		assertThat(testConfigurer.getMaxInactiveIntervalInSeconds()).isEqualTo(300);
		assertThat(testConfigurer.getPoolName()).isEqualTo("MockPool");
		assertThat(testConfigurer.getRegionName()).isEqualTo("MockRegion");
		assertThat(testConfigurer.getSessionExpirationPolicyBeanName()).isEqualTo("MockExpirationPolicy");
		assertThat(testConfigurer.getSessionSerializerBeanName()).isEqualTo("MockSerializer");

		Method[] declaredMethods = filterDeclaredMethods(testConfigurer.getClass().getDeclaredMethods());

		List<String> declaredMethodNames =
			Arrays.stream(declaredMethods).map(Method::getName).sorted().collect(Collectors.toList());

		assertThat(declaredMethods).isNotNull();
		assertThat(declaredMethods).hasSize(6);

		assertThat(declaredMethodNames)
			.containsExactly("getClientRegionShortcut",
				"getMaxInactiveIntervalInSeconds", "getPoolName", "getRegionName",
				"getSessionExpirationPolicyBeanName", "getSessionSerializerBeanName");
	}

	@Test
	public void classGetDeclaredMethodsOnCustomConfigurerObjectHasNoMethods() {

		SpringSessionGemFireConfigurer testConfigurer = newTestConfigurerWithNoOverrides();

		assertThat(testConfigurer).isNotNull();
		assertThat(testConfigurer.getClientRegionShortcut())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_CLIENT_REGION_SHORTCUT);
		assertThat(testConfigurer.getMaxInactiveIntervalInSeconds())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_MAX_INACTIVE_INTERVAL_IN_SECONDS);
		assertThat(testConfigurer.getPoolName())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_POOL_NAME);
		assertThat(testConfigurer.getRegionName())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_SESSION_REGION_NAME);
		assertThat(testConfigurer.getSessionExpirationPolicyBeanName())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_SESSION_EXPIRATION_POLICY_BEAN_NAME);
		assertThat(testConfigurer.getSessionSerializerBeanName())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_SESSION_SERIALIZER_BEAN_NAME);

		Method[] declaredMethods = filterDeclaredMethods(testConfigurer.getClass().getDeclaredMethods());

		assertThat(declaredMethods).isNotNull();
		assertThat(declaredMethods).isEmpty();
	}

	@Test
	public void classGetDeclaredMethodsOnCustomConfigurerObjectOnlyHasOverriddenMethods() {

		SpringSessionGemFireConfigurer testConfigurer = newTestConfigurerWithSelectOverrides();

		assertThat(testConfigurer).isNotNull();
		assertThat(testConfigurer.getClientRegionShortcut()).isEqualTo(ClientRegionShortcut.CACHING_PROXY);
		assertThat(testConfigurer.getMaxInactiveIntervalInSeconds()).isEqualTo(600);
		assertThat(testConfigurer.getPoolName()).isEqualTo("TestPool");
		assertThat(testConfigurer.getRegionName()).isEqualTo("TestRegion");
		assertThat(testConfigurer.getSessionExpirationPolicyBeanName()).isEmpty();
		assertThat(testConfigurer.getSessionSerializerBeanName())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_SESSION_SERIALIZER_BEAN_NAME);

		Method[] declaredMethods = filterDeclaredMethods(testConfigurer.getClass().getDeclaredMethods());

		List<String> declaredMethodNames =
			Arrays.stream(declaredMethods).map(Method::getName).sorted().collect(Collectors.toList());

		assertThat(declaredMethods).isNotNull();
		assertThat(declaredMethods).hasSize(4);

		assertThat(declaredMethodNames)
			.containsExactly("getClientRegionShortcut", "getMaxInactiveIntervalInSeconds",
				"getPoolName", "getRegionName");

		assertThat(declaredMethodNames)
			.doesNotContain("getSessionExpirationPolicyBeanName", "getSessionSerializerBeanName");
	}
}
