/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.data;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.apache.geode.cache.client.ClientCache;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests testing the configuration of client-side Data Serialization with no explicit server-side
 * Serialization configuration.
 *
 * @author John Blum
 * @see Serializable
 * @see Test
 * @see ClientCache
 * @see AnnotationConfigApplicationContext
 * @see Bean
 * @see CacheServerApplication
 * @see ClientCacheApplication
 * @see Session
 * @see SessionRepository
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SessionSerializationWithDataSerializationAndNoServerSerializationConfigurationIntegrationTests.ClientTestConfiguration.class)
public class SessionSerializationWithDataSerializationAndNoServerSerializationConfigurationIntegrationTests {

	private static final String SESSIONS_REGION_NAME = "Sessions";

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {

		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withCacheXml(GemFireCluster.ALL_GLOB, "/session-serializer-cache.xml")
				.withClasspath(GemFireCluster.ALL_GLOB, System.getProperty("TEST_JAR_PATH"))
				.withGfsh(false, "create region --name=Sessions --type=PARTITION");

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
	public void setup() {

		assertThat(this.clientCache).isNotNull();
		assertThat(this.clientCache.getPdxSerializer()).isNull();
		assertThat(this.sessionRepository).isNotNull();
	}

	@Test
	public void serializesSessionStateCorrectly() {

		Session session = this.sessionRepository.createSession();

		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isExpired()).isFalse();
		assertThat(session.getAttributeNames()).isEmpty();

		User jonDoe = new User(1, "jonDoe");

		session.setAttribute("user", jonDoe);

		assertThat(session.getAttributeNames()).containsExactly("user");
		assertThat(session.<User>getAttribute("user")).isEqualTo(jonDoe);

		// Saves the entire Session object
		this.sessionRepository.save(session);

		User janeDoe = new User(2, "janeDoe");

		session.setAttribute("user", janeDoe);

		assertThat(session.getAttributeNames()).containsExactly("user");
		assertThat(session.<User>getAttribute("user")).isEqualTo(janeDoe);

		// Saves only the delta
		this.sessionRepository.save(session);

		Session loadedSession = this.sessionRepository.findById(session.getId());

		assertThat(loadedSession).isNotNull();
		assertThat(loadedSession).isNotSameAs(session);
		assertThat(loadedSession).isEqualTo(session);
		assertThat(loadedSession.getAttributeNames()).containsExactly("user");
		assertThat(loadedSession.<User>getAttribute("user")).isEqualTo(janeDoe);
	}

	@ClientCacheApplication
	@EnableGemFireHttpSession(
		poolName = "DEFAULT",
		regionName = SESSIONS_REGION_NAME,
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	static class ClientTestConfiguration { }
	
  record User(Integer id, String name) implements Serializable {
	}
}
