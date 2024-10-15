/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import jakarta.annotation.Resource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.InterestResultPolicy;
import org.apache.geode.cache.Region;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests using the Apache Geode client/server topology to test that no client subscriptions
 * or register interests calls are made when HTTP {@link Session} events are disabled.
 *
 * @author John Blum
 * @see Test
 * @see org.mockito.Mockito
 * @see org.mockito.Spy
 * @see Region
 * @see AnnotationConfigApplicationContext
 * @see Bean
 * @see ClientCacheApplication
 * @see Session
 * @see EnableGemFireHttpSession
 * @see GemFireHttpSessionConfiguration
 * @see AbstractGemFireIntegrationTests
 * @see GemFireOperationsSessionRepository
 * @see AbstractSessionEvent
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.2.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = DisabledClientSubscriptionsIntegrationTests.TestSpringSessionGemFireClientConfiguration.class
)
@SuppressWarnings("unused")
public class DisabledClientSubscriptionsIntegrationTests extends AbstractGemFireIntegrationTests {

	private static final int MAX_INACTIVE_INTERVAL_IN_SECONDS = 1;

	private static final String GEMFIRE_LOG_LEVEL = "error";
	private static final String TEST_SESSION_REGION_NAME = "TestNonInterestingSessions";

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withGfsh(false, "create region --type=PARTITION --name=" + TEST_SESSION_REGION_NAME);

		gemFireCluster.acceptLicense().start();

		System.setProperty("spring.data.gemfire.pool.locators", String.format("localhost[%d]", gemFireCluster.getLocatorPort()));
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	@Resource(name = GemFireHttpSessionConfiguration.DEFAULT_SESSION_REGION_NAME)
	private Region<?, ?> sessions;

	@Autowired
	private SessionEventListener sessionEventListener;

	@Before
	public void setup() {

		assertThat(this.sessions).isNotNull();
		assertThat(this.sessionRepository).isInstanceOf(GemFireOperationsSessionRepository.class);
		assertThat(((GemFireOperationsSessionRepository) this.sessionRepository).getMaxInactiveIntervalInSeconds())
			.isEqualTo(MAX_INACTIVE_INTERVAL_IN_SECONDS);
		assertThat(((GemFireOperationsSessionRepository) this.sessionRepository).getSessionsRegionName())
			.isEqualTo(this.sessions.getFullPath());
	}

	@After
	@SuppressWarnings("unchecked")
	public void tearDown() {

		verify(this.sessions, never()).registerInterest(any());
		verify(this.sessions, never()).registerInterest(any(), anyBoolean());
		verify(this.sessions, never()).registerInterest(any(), anyBoolean(), anyBoolean());
		verify(this.sessions, never()).registerInterest(any(), anyBoolean(), anyBoolean());
		verify(this.sessions, never()).registerInterest(any(), any(InterestResultPolicy.class));
		verify(this.sessions, never()).registerInterest(any(), any(InterestResultPolicy.class), anyBoolean());
		verify(this.sessions, never()).registerInterest(any(), any(InterestResultPolicy.class), anyBoolean(), anyBoolean());
		verify(this.sessions, never()).registerInterestForAllKeys();
		verify(this.sessions, never()).registerInterestForAllKeys(any(InterestResultPolicy.class));
		verify(this.sessions, never()).registerInterestForAllKeys(any(InterestResultPolicy.class), anyBoolean());
		verify(this.sessions, never()).registerInterestForAllKeys(any(InterestResultPolicy.class), anyBoolean(), anyBoolean());
		verify(this.sessions, never()).registerInterestForKeys(any(Iterable.class));
		verify(this.sessions, never()).registerInterestForKeys(any(Iterable.class), any(InterestResultPolicy.class));
		verify(this.sessions, never()).registerInterestForKeys(any(Iterable.class), any(InterestResultPolicy.class), anyBoolean());
		verify(this.sessions, never()).registerInterestForKeys(any(Iterable.class), any(InterestResultPolicy.class), anyBoolean(), anyBoolean());
		verify(this.sessions, never()).registerInterestRegex(anyString());
		verify(this.sessions, never()).registerInterestRegex(anyString(), anyBoolean());
		verify(this.sessions, never()).registerInterestRegex(anyString(), anyBoolean(), anyBoolean());
		verify(this.sessions, never()).registerInterestRegex(anyString(), any(InterestResultPolicy.class));
		verify(this.sessions, never()).registerInterestRegex(anyString(), any(InterestResultPolicy.class), anyBoolean());
		verify(this.sessions, never()).registerInterestRegex(anyString(), any(InterestResultPolicy.class), anyBoolean(), anyBoolean());
		verify(this.sessions, never()).unregisterInterest(any());
		verify(this.sessions, never()).unregisterInterestRegex(anyString());
	}

	@Test
	public void sessionEventsFireSuccessfully() {

		Session expectedSession = save(createSession());

		assertThat(expectedSession).isNotNull();
		assertThat(expectedSession.isExpired()).isFalse();
		assertThat(expectedSession.getId()).isNotEmpty();
		assertThat(expectedSession.getMaxInactiveInterval())
			.isEqualTo(Duration.ofSeconds(MAX_INACTIVE_INTERVAL_IN_SECONDS));

		// Wait for Session created event
		AbstractSessionEvent sessionEvent = this.sessionEventListener.waitForSessionEvent(500L);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.<Session>getSession()).isEqualTo(expectedSession);

		Session actualSession = get(expectedSession.getId());

		assertThat(actualSession).isNotNull();
		assertThat(actualSession).isEqualTo(expectedSession);
		assertThat(actualSession).isNotSameAs(expectedSession);

		delete(actualSession);

		actualSession = get(actualSession.getId());

		assertThat(actualSession).isNull();

		// Wait for Session deleted event
		sessionEvent = this.sessionEventListener.waitForSessionEvent(500L);

		assertThat(sessionEvent).isInstanceOf(SessionDeletedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(expectedSession.getId());
	}

	@Test
	public void sessionExpirationIsNotFired() {

		Session expectedSession = save(createSession());

		assertThat(expectedSession).isNotNull();
		assertThat(expectedSession.isExpired()).isFalse();
		assertThat(expectedSession.getId()).isNotEmpty();
		assertThat(expectedSession.getMaxInactiveInterval())
			.isEqualTo(Duration.ofSeconds(MAX_INACTIVE_INTERVAL_IN_SECONDS));

		AbstractSessionEvent sessionEvent = this.sessionEventListener.waitForSessionEvent(500L);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.<Session>getSession()).isEqualTo(expectedSession);

		Session actualSession = get(expectedSession.getId());

		assertThat(actualSession).isNotNull();
		assertThat(actualSession).isEqualTo(expectedSession);
		assertThat(actualSession).isNotSameAs(expectedSession);

		long duration = Duration.ofSeconds(MAX_INACTIVE_INTERVAL_IN_SECONDS).toMillis() * 2L;

		sessionEvent = this.sessionEventListener.waitForSessionEvent(duration);

		assertThat(sessionEvent).isNull();

		actualSession = get(expectedSession.getId());

		assertThat(actualSession).isNull();
	}

	@ClientCacheApplication(
		name = "DisabledClientSubscriptionsIntegrationTests",
		logLevel = GEMFIRE_LOG_LEVEL,
		copyOnRead = true
	)
	@EnableGemFireHttpSession(
		regionName = TEST_SESSION_REGION_NAME,
		poolName = "DEFAULT",
		maxInactiveIntervalInSeconds = MAX_INACTIVE_INTERVAL_IN_SECONDS
	)
	static class TestSpringSessionGemFireClientConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		BeanPostProcessor sessionRegionSpyBeanPostProcessor() {

			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

					if (bean instanceof Region) {

						Region<Object, Session> region = (Region<Object, Session>) bean;

						bean = TEST_SESSION_REGION_NAME.equals(region.getName()) ? spy(region) : region;
					}

					return bean;
				}
			};
		}

		@Bean
		SessionEventListener sessionEventListener() {
			return new SessionEventListener();
		}
	}
}
