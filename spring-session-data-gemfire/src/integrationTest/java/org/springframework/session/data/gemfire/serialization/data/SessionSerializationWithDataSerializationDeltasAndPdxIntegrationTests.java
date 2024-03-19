/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.NotSerializableException;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.DeltaSerializationException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer;
import org.springframework.data.gemfire.mapping.MappingPdxSerializer;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration test testing the serialization of a {@link Session} object containing application domain object types
 * de/serialized using PDX.
 *
 * @author John Blum
 * @see Test
 * @see AnnotationConfigApplicationContext
 * @see Bean
 * @see CacheServerApplication
 * @see ClientCacheApplication
 * @see EnablePdx
 * @see PeerCacheConfigurer
 * @see MappingPdxSerializer
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.3
 */
@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SessionSerializationWithDataSerializationDeltasAndPdxIntegrationTests.GemFireClientConfiguration.class)
public class SessionSerializationWithDataSerializationDeltasAndPdxIntegrationTests
		extends AbstractGemFireIntegrationTests {

	private CacheFactoryBean cache;

	private static final String GEMFIRE_LOG_LEVEL = "error";

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withCacheXml(GemFireCluster.ALL_GLOB, "/session-serializer-cache.xml")
				.withClasspath(GemFireCluster.ALL_GLOB, System.getProperty("TEST_JAR_PATH"))
				.withPdx("org\\.springframework\\.session\\.data\\.gemfire\\.serialization\\.data\\..*", true)
				.withGfsh(false, "create region --type=PARTITION --name=ClusteredSpringSessions");

		gemFireCluster.acceptLicense().start();

		System.setProperty("spring.data.gemfire.pool.locators", String.format("localhost[%d]", gemFireCluster.getLocatorPort()));
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	@Test
	public void sessionDataSerializationWithCustomerPdxSerializationWorksAsExpected() {

		Customer jonDoe = new Customer("Jon Doe");
		Customer janeDoe = new Customer("Jane Doe");

		Session session = createSession();

		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isExpired()).isFalse();
		assertThat(session.getAttributeNames()).isEmpty();

		session.setAttribute("1", jonDoe);

		assertThat(session.getAttributeNames()).containsExactly("1");
		assertThat(session.<Customer>getAttribute("1")).isEqualTo(jonDoe);

		save(touch(session));

		Session loadedSession = get(session.getId());

		assertThat(loadedSession).isNotNull();
		assertThat(loadedSession).isNotSameAs(session);
		assertThat(loadedSession.getId()).isEqualTo(session.getId());
		assertThat(loadedSession.isExpired()).isFalse();
		assertThat(loadedSession.getAttributeNames()).containsExactly("1");
		assertThat(loadedSession.<Customer>getAttribute("1")).isEqualTo(jonDoe);
		assertThat(loadedSession.<Customer>getAttribute("1")).isNotSameAs(jonDoe);

		loadedSession.setAttribute("2", janeDoe);

		assertThat(loadedSession.getAttributeNames()).containsOnly("1", "2");
		assertThat(loadedSession.<Customer>getAttribute("1")).isEqualTo(jonDoe);
		assertThat(loadedSession.<Customer>getAttribute("2")).isEqualTo(janeDoe);

		save(touch(loadedSession));

		Session reloadedSession = get(loadedSession.getId());

		assertThat(reloadedSession).isNotNull();
		assertThat(reloadedSession).isNotSameAs(loadedSession);
		assertThat(reloadedSession.getId()).isEqualTo(loadedSession.getId());
		assertThat(reloadedSession.isExpired()).isFalse();
		assertThat(reloadedSession.getAttributeNames()).containsOnly("1", "2");
		assertThat(reloadedSession.<Customer>getAttribute("1")).isEqualTo(jonDoe);
		assertThat(reloadedSession.<Customer>getAttribute("2")).isEqualTo(janeDoe);
	}

	@Test(expected = DeltaSerializationException.class)
	public void serializationOfNonSerializableTypeThrowsException() {

		NonSerializableType value = new NonSerializableType("test");

		Session session = createSession();

		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isExpired()).isFalse();
		assertThat(session.getAttributeNames()).isEmpty();

		session.setAttribute("9", value);

		assertThat(session.getAttributeNames()).containsExactly("9");
		assertThat(session.<NonSerializableType>getAttribute("9")).isEqualTo(value);

		try {
			save(touch(session));
		}
		catch (Exception expected) {

			assertThat(expected).isInstanceOf(DeltaSerializationException.class);
			assertThat(expected).hasCauseInstanceOf(NotSerializableException.class);
			assertThat(expected.getCause()).hasMessageContaining(NonSerializableType.class.getName());

			throw expected;
		}
	}

	@ClientCacheApplication(logLevel = GEMFIRE_LOG_LEVEL, subscriptionEnabled = true)
	@EnableGemFireHttpSession(
		poolName = "DEFAULT",
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	@EnablePdx(serializerBeanName = "customerPdxSerializer")
	static class GemFireClientConfiguration {

		@Bean
		MappingPdxSerializer customerPdxSerializer() {

			MappingPdxSerializer pdxSerializer = new MappingPdxSerializer();

			pdxSerializer.setIncludeTypeFilters(type -> type != null && Customer.class.isAssignableFrom(type));

			return pdxSerializer;
		}
	}

	record Customer(String name) { }

	record NonSerializableType(String value) { }
}
