/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.EntryEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the proper behavior of Spring Session for Apache Geode & Pivotal GemFire
 * Session event handling, and specifically, translation between GemFire/Cache cache {@link EntryEvent EntryEvents}
 * and Spring container {@link ApplicationEvent ApplicationEvents}.
 *
 * @author John Blum
 * @see EntryEvent
 * @see Test
 * @see ApplicationEvent
 * @see AnnotationConfigApplicationContext
 * @see ClientCacheApplication
 * @see EnableGemFireHttpSession
 * @see SessionCreatedEvent
 * @see SessionDeletedEvent
 * @see SessionExpiredEvent
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 1.1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = ClientServerProxyRegionSessionOperationsIntegrationTests.SpringSessionDataGemFireClientConfiguration.class
)
public class ClientServerProxyRegionSessionOperationsIntegrationTests extends AbstractGemFireIntegrationTests {

	private static final int MAX_INACTIVE_INTERVAL_IN_SECONDS = 1;

	private static GemFireCluster gemFireCluster;

	@Autowired
	@SuppressWarnings("unused")
	private SessionEventListener sessionEventListener;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withGfsh(false, "create region --name=ClusteredSpringSessions --type=PARTITION --enable-statistics " +
						"--entry-idle-time-expiration-action=INVALIDATE --entry-idle-time-expiration=" + MAX_INACTIVE_INTERVAL_IN_SECONDS);

		gemFireCluster.acceptLicense().start();

		System.setProperty("spring.data.gemfire.pool.locators", String.format("localhost[%d]", gemFireCluster.getLocatorPort()));
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	@Test
	public void createReadUpdateExpireRecreateDeleteRecreateSessionResultsCorrectSessionCreatedEvents() {

		// CREATE
		Session session = save(touch(createSession()));

		assertValidSession(session);

		AbstractSessionEvent sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// GET
		Session loadedSession = get(session.getId());

		assertThat(loadedSession).isNotNull();
		assertThat(loadedSession.getId()).isEqualTo(session.getId());
		// TODO: Problem on Java 17
		//assertThat(loadedSession.getCreationTime()).isEqualTo(session.getCreationTime());
		assertThat(loadedSession.getCreationTime().toEpochMilli()).isEqualTo(session.getCreationTime().toEpochMilli());
		assertThat(loadedSession.getLastAccessedTime().compareTo(session.getLastAccessedTime()))
			.isGreaterThanOrEqualTo(0);

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isNull();

		loadedSession.setAttribute("attrOne", 1);
		loadedSession.setAttribute("attrTwo", 2);

		// UPDATE
		save(touch(loadedSession));

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isNull();

		// EXPIRE
		sessionEvent = this.sessionEventListener.waitForSessionEvent(
			TimeUnit.SECONDS.toMillis(MAX_INACTIVE_INTERVAL_IN_SECONDS + 1));

		assertThat(sessionEvent).isInstanceOf(SessionExpiredEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// RECREATE
		save(touch(session));

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// DELETE
		delete(session);

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionDeletedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// RECREATE
		save(touch(session));

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());
	}

	@ClientCacheApplication(
		pingInterval = 5000,
		readTimeout = 2000,
		retryAttempts = 1,
		subscriptionEnabled = true
	)
	@EnableGemFireHttpSession(poolName = "DEFAULT")
	@SuppressWarnings("unused")
	static class SpringSessionDataGemFireClientConfiguration {

		@Bean
		public SessionEventListener sessionEventListener() {
			return new SessionEventListener();
		}
	}
}
