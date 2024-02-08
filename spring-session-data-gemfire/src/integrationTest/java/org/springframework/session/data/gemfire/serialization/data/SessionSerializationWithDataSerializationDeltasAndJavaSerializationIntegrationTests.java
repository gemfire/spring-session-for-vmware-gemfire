/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Serializable;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration test testing the serialization of a {@link Session} object containing application domain object types
 * de/serialized using Java Serialization.
 *
 * @author John Blum
 * @see Serializable
 * @see ClientCache
 * @see AnnotationConfigApplicationContext
 * @see CacheServerApplication
 * @see ClientCacheApplication
 * @see Session
 * @see SessionRepository
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = SessionSerializationWithDataSerializationDeltasAndJavaSerializationIntegrationTests.GemFireClientConfiguration.class
)
@SuppressWarnings("unused")
public class SessionSerializationWithDataSerializationDeltasAndJavaSerializationIntegrationTests
		extends AbstractGemFireIntegrationTests {

	private static final String GEMFIRE_LOG_LEVEL = "error";
	private static final String SESSIONS_REGION_NAME = "Sessions";

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withCacheXml(GemFireCluster.ALL_GLOB, "/session-serializer-cache.xml")
				.withClasspath(GemFireCluster.ALL_GLOB, System.getProperty("TEST_JAR_PATH"))
				.withGfsh(false, "create region --type=PARTITION --name=" + SESSIONS_REGION_NAME);

		gemFireCluster.acceptLicense().start();

		System.setProperty("spring.data.gemfire.pool.locators", String.format("localhost[%d]", gemFireCluster.getLocatorPort()));
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	@Autowired
	private ClientCache clientCache;

	@Autowired
	private SessionRepository<Session> sessionRepository;

	@Before
	public void assertPdxNotConfigured() {

		assertThat(this.clientCache).isNotNull();
		assertThat(this.clientCache.getPdxSerializer()).isNull();
	}

	@Test
	public void serializesSessionStateCorrectly() {

		Session session = this.sessionRepository.createSession();

		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isExpired()).isFalse();
		assertThat(session.getAttributeNames()).isEmpty();

		Customer jonDoe = new Customer("Jon Doe");

		session.setAttribute("jonDoe", jonDoe);

		assertThat(session.getAttributeNames()).containsOnly("jonDoe");
		assertThat(session.<Customer>getAttribute("jonDoe")).isEqualTo(jonDoe);

		this.sessionRepository.save(session);

		Customer janeDoe = new Customer("Jane Doe");

		session.setAttribute("janeDoe", janeDoe);

		assertThat(session.getAttributeNames()).containsOnly("jonDoe", "janeDoe");
		assertThat(session.<Customer>getAttribute("jonDoe")).isEqualTo(jonDoe);
		assertThat(session.<Customer>getAttribute("janeDoe")).isEqualTo(janeDoe);

		this.sessionRepository.save(session);

		Session loadedSession = this.sessionRepository.findById(session.getId());

		assertThat(loadedSession).isEqualTo(session);
		assertThat(loadedSession).isNotSameAs(session);
		assertThat(loadedSession.getAttributeNames()).containsOnly("jonDoe", "janeDoe");
		assertThat(loadedSession.<Customer>getAttribute("jonDoe")).isEqualTo(jonDoe);
		assertThat(loadedSession.<Customer>getAttribute("janeDoe")).isEqualTo(janeDoe);
	}

	@ClientCacheApplication(logLevel = GEMFIRE_LOG_LEVEL, subscriptionEnabled = true)
	@EnableGemFireHttpSession(
		poolName = "DEFAULT",
		regionName = SESSIONS_REGION_NAME,
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	static class GemFireClientConfiguration { }

	record Customer(String name) implements Serializable {

		/*
		// Uncomment this method to see exactly how/where GemFire tries to deserialize the Customer object
		// and resolve the Customer class on the server-side.
		private void readObject(ObjectInputStream inputStream) throws IOException {
			throw new IllegalStateException("Customer could not be deserialized");
		}
		*/
	}
}
