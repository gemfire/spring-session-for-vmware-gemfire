/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.support.GemFireUtils;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.shaded.org.awaitility.Awaitility;

/**
 * Integration Test to test the functionality of a Pivotal GemFire cache client in a Spring Session application
 * using a specifically named Pivotal GemFire {@link org.apache.geode.cache.client.Pool} configured with
 * the 'poolName' attribute in the Spring Session Data Pivotal GemFire {@link EnableGemFireHttpSession} annotation.
 *
 * @author John Blum
 * @see Test
 * @see RunWith
 * @see AbstractGemFireIntegrationTests
 * @see EnableGemFireHttpSession
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration
 * @see DirtiesContext
 * @see ContextConfiguration
 * @see SpringRunner
 * @see WebAppConfiguration
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.server.CacheServer
 * @since 1.3.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = MultiPoolClientServerGemFireOperationsSessionRepositoryIntegrationTests.SpringSessionDataGemFireClientConfiguration.class
)
@DirtiesContext
@WebAppConfiguration
public class MultiPoolClientServerGemFireOperationsSessionRepositoryIntegrationTests
		extends AbstractGemFireIntegrationTests {

	private static final int MAX_INACTIVE_INTERVAL_IN_SECONDS = 1;

	private static final String SPRING_SESSION_GEMFIRE_REGION_NAME = "TestMultiPoolClientServerSessions";

	@Autowired
	private SessionEventListener sessionEventListener;

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withGfsh(true, "create region --type=PARTITION --name=" + SPRING_SESSION_GEMFIRE_REGION_NAME
				 + " --enable-statistics --entry-idle-time-expiration-action=INVALIDATE --entry-idle-time-expiration=" + MAX_INACTIVE_INTERVAL_IN_SECONDS);

		gemFireCluster.acceptLicense().start();
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	@Before
	public void setup() {

		Region<Object, Session> springSessionGemFireRegion =
			gemfireCache.getRegion(SPRING_SESSION_GEMFIRE_REGION_NAME);

		assertThat(springSessionGemFireRegion).isNotNull();

		RegionAttributes<Object, Session> springSessionGemFireRegionAttributes =
			springSessionGemFireRegion.getAttributes();

		assertThat(springSessionGemFireRegionAttributes).isNotNull();
		assertThat(springSessionGemFireRegionAttributes.getDataPolicy()).isEqualTo(DataPolicy.EMPTY);
	}

	protected static ConnectionEndpoint newConnectionEndpoint(String host, int port) {
		return new ConnectionEndpoint(host, port);
	}

	@Test
	public void getExistingNonExpiredSessionBeforeAndAfterExpiration() {

		Session expectedSession = save(touch(createSession()));

		AbstractSessionEvent sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.<Session>getSession()).isEqualTo(expectedSession);
		assertThat(this.sessionEventListener.<SessionCreatedEvent>getSessionEvent()).isNull();

		Session savedSession = this.gemfireSessionRepository.findById(expectedSession.getId());

		assertThat(savedSession).isEqualTo(expectedSession);

		this.sessionEventListener.getSessionEvent();

		Awaitility.await().pollDelay(Duration.ofSeconds(MAX_INACTIVE_INTERVAL_IN_SECONDS + 1)).untilAsserted(() -> {
			sessionRepository.findById(expectedSession.getId());

			Optional<AbstractSessionEvent> event = Optional.ofNullable(this.sessionEventListener.waitForSessionEvent(
					TimeUnit.SECONDS.toMillis(MAX_INACTIVE_INTERVAL_IN_SECONDS + 1)));

			event.ifPresent(abstractSessionEvent -> {
				assertThat(abstractSessionEvent).isInstanceOf(SessionExpiredEvent.class);
				assertThat(abstractSessionEvent.getSessionId()).isEqualTo(expectedSession.getId());
			});
		});

		Session expiredSession = this.gemfireSessionRepository.findById(expectedSession.getId());

		assertThat(expiredSession).isNull();
	}

	@ClientCacheApplication(logLevel = "error")
	@EnableGemFireHttpSession(
		regionName = SPRING_SESSION_GEMFIRE_REGION_NAME,
		poolName = "serverPool",
		maxInactiveIntervalInSeconds = MAX_INACTIVE_INTERVAL_IN_SECONDS
	)
	@SuppressWarnings("unused")
	static class SpringSessionDataGemFireClientConfiguration {

		@Bean
		PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		PoolFactoryBean gemfirePool() {

			PoolFactoryBean poolFactory = new PoolFactoryBean();

			poolFactory.setFreeConnectionTimeout(5000); // 5 seconds
			poolFactory.setKeepAlive(false);
			poolFactory.setMinConnections(0);
			poolFactory.setReadTimeout(500);

			// deliberately set to a non-existing Pivotal GemFire (Cache) Server
			poolFactory.addServers(newConnectionEndpoint("localhost", 53135));

			return poolFactory;
		}

		@Bean
		PoolFactoryBean serverPool() {

			PoolFactoryBean poolFactory = new PoolFactoryBean();

			poolFactory.setFreeConnectionTimeout(5000); // 5 seconds
			poolFactory.setKeepAlive(false);
			poolFactory.setMaxConnections(50);
			poolFactory.setMinConnections(1);
			poolFactory.setPingInterval(TimeUnit.SECONDS.toMillis(5));
			poolFactory.setReadTimeout(2000); // 2 seconds
			poolFactory.setRetryAttempts(1);
			poolFactory.setSubscriptionEnabled(true);
			poolFactory.addServers(newConnectionEndpoint("localhost", gemFireCluster.getServerPorts().get(0)));

			return poolFactory;
		}

		@Bean
		public SessionEventListener sessionEventListener() {
			return new SessionEventListener();
		}
	}
}
