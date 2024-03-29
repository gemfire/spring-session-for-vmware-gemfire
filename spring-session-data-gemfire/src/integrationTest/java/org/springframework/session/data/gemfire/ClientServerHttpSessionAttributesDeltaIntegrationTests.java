/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests testing the addition/removal of HTTP Session Attributes
 * and the proper persistence of the HTTP Session state in a Pivotal GemFire cache
 * across a client/server topology.
 *
 * @author John Blum
 * @see Test
 * @see RunWith
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see ContextConfiguration
 * @see SpringRunner
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @since 1.3.1
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = ClientServerHttpSessionAttributesDeltaIntegrationTests.SpringSessionDataGemFireClientConfiguration.class
)
public class ClientServerHttpSessionAttributesDeltaIntegrationTests extends AbstractGemFireIntegrationTests {

  private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withCacheXml(GemFireCluster.ALL_GLOB, "/session-serializer-cache.xml")
				.withClasspath(GemFireCluster.ALL_GLOB, System.getProperty("TEST_JAR_PATH"))
				.withGfsh(false, "create region --type=PARTITION --name=ClusteredSpringSessions");

		gemFireCluster.acceptLicense().start();

		System.setProperty("spring.data.gemfire.pool.locators", String.format("localhost[%d]", gemFireCluster.getLocatorPort()));
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	@Test
	public void sessionDeltaOperationsAreCorrect() {

		Session session = save(touch(createSession()));

		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.isExpired()).isFalse();
		assertThat(session.getAttributeNames()).isEmpty();

		session.setAttribute("attrOne", 1);
		session.setAttribute("attrTwo", 2);

		save(touch(session));

		Session loadedSession = get(session.getId());

		assertThat(loadedSession).isNotNull();
		assertThat(loadedSession).isNotSameAs(session);
		assertThat(loadedSession.getId()).isEqualTo(session.getId());
		assertThat(loadedSession.isExpired()).isFalse();
		assertThat(loadedSession.<Integer>getAttribute("attrOne")).isEqualTo(1);
		assertThat(loadedSession.<Integer>getAttribute("attrTwo")).isEqualTo(2);

		loadedSession.removeAttribute("attrTwo");

		assertThat(loadedSession.getAttributeNames()).containsOnly("attrOne");
		assertThat(loadedSession.getAttributeNames()).doesNotContain("attrTwo");

		save(touch(loadedSession));

		Session reloadedSession = get(loadedSession.getId());

		assertThat(reloadedSession).isNotNull();
		assertThat(reloadedSession).isNotSameAs(loadedSession);
		assertThat(reloadedSession.isExpired()).isFalse();
		assertThat(reloadedSession.getId()).isEqualTo(loadedSession.getId());
		assertThat(reloadedSession.getAttributeNames()).containsOnly("attrOne");
		assertThat(reloadedSession.getAttributeNames()).doesNotContain("attrTwo");
		assertThat(reloadedSession.<Integer>getAttribute("attrOne")).isEqualTo(1);
	}

	@ClientCacheApplication(
		logLevel = "error",
		subscriptionEnabled = true
	)
	@EnableGemFireHttpSession(
		poolName = "DEFAULT",
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	static class SpringSessionDataGemFireClientConfiguration { }
}
