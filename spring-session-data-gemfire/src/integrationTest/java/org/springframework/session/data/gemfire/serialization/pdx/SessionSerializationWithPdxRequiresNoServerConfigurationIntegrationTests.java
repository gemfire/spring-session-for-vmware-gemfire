/*
 * Copyright (c) VMware, Inc. 2022-2024. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.pdx;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.server.CacheServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSession;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.server.GemFireServer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

/**
 * Integration tests asserting that a GemFire/Geode Server does not require any Spring Session Data GemFire/Geode
 * dependencies or any transitive dependencies when PDX serialization is used.
 *
 * @author John Blum
 * @see Test
 * @see File
 * @see Instant
 * @see CacheServer
 * @see Bean
 * @see PropertySourcesPlaceholderConfigurer
 * @see ClientCacheApplication
 * @see ClientCacheConfigurer
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see GemFireOperationsSessionRepository
 * @see EnableGemFireHttpSession
 * @see GemFireServer
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class SessionSerializationWithPdxRequiresNoServerConfigurationIntegrationTests
		extends AbstractGemFireIntegrationTests {

	private static final DateFormat TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	private static File processWorkingDirectory;

	private static Process gemfireServer;

	private static final String GEMFIRE_LOG_LEVEL = "error";

	@Autowired
	private GemFireOperationsSessionRepository sessionRepository;

	@BeforeClass
	public static void startGemFireServer() throws IOException {

		long t0 = System.currentTimeMillis();

		int port = findAndReserveAvailablePort();

		System.setProperty("spring.session.data.gemfire.cache.server.port", String.valueOf(port));

		String classpath = buildClassPathContainingJarFiles("javax.transaction-api", "antlr",
			"commons-lang", "commons-io", "commons-validator", "fastutil", "log4j-api", "log4j-to-slf4j",
			"gemfire-common", "gemfire-core", "gemfire-deployment-chained-classloader", "gemfire-logging", "gemfire-management", "gemfire-membership",
			"gemfire-serialization", "gemfire-tcp-server","gemfire-tcp-messenger", "jgroups", "micrometer-core", "micrometer-commons", "rmiio", "gemfire-version",
			"shiro-core", "slf4j-api");

		String processWorkingDirectoryPathname =
			String.format("gemfire-server-pdx-serialization-tests-%1$s", TIMESTAMP.format(new Date()));

		processWorkingDirectory = createDirectory(processWorkingDirectoryPathname);

		gemfireServer = run(classpath, GemFireServer.class,
			processWorkingDirectory, String.format("-Dspring.session.data.gemfire.cache.server.port=%d", port),
				String.format("-Dgemfire.log-level=%s", GEMFIRE_LOG_LEVEL));

 		assertThat(waitForServerToStart("localhost", port)).isTrue();

		//System.err.printf("GemFire Server [startup time = %d ms]%n", System.currentTimeMillis() - t0);
	}

	@AfterClass
	public static void stopGemFireServer() {

		Optional.ofNullable(gemfireServer).ifPresent(server -> {

			server.destroy();

			System.err.printf("GemFire Server [exit code = %d]%n",
				waitForProcessToStop(server, processWorkingDirectory));
		});

		boolean forkClean = !System.getProperties().containsKey("spring.session.data.gemfire.fork.clean")
			|| Boolean.getBoolean("spring.session.data.gemfire.fork.clean");

		if (forkClean) {
			FileSystemUtils.deleteRecursively(processWorkingDirectory);
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void sessionOperationsIsSuccessful() {

		Session session = save(createSession("jonDoe"));

		assertThat(session).isInstanceOf(GemFireSession.class);
		assertThat(session.getId()).isNotNull();
		assertThat(session.getCreationTime()).isBeforeOrEqualTo(Instant.now());
		assertThat(session.isExpired()).isFalse();
		assertThat(((GemFireSession) session).getPrincipalName()).isEqualTo("jonDoe");

		Session sessionById = get(session.getId());

		assertThat(sessionById).isEqualTo(session);
		assertThat(sessionById).isNotSameAs(session);
		assertThat(sessionById.isExpired()).isFalse();

		Map<String, Session> sessionsByPrincipalName = this.sessionRepository.findByPrincipalName("jonDoe");

		assertThat(sessionsByPrincipalName).hasSize(1);

		Session sessionByPrincipalName = sessionsByPrincipalName.values().iterator().next();

		assertThat(sessionByPrincipalName).isInstanceOf(GemFireSession.class);
		assertThat(sessionByPrincipalName).isEqualTo(session);
		assertThat(sessionByPrincipalName).isNotSameAs(session);
		assertThat(sessionByPrincipalName.isExpired()).isFalse();
		assertThat(((GemFireSession) sessionByPrincipalName).getPrincipalName()).isEqualTo("jonDoe");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void operationsOnSessionContainingApplicationDomainModelObjectIsSuccessful() {

		UsernamePasswordAuthenticationToken jxblumToken =
			new UsernamePasswordAuthenticationToken("jxblum", "p@55w0rd");

		Session session = createSession("janeDoe");

		assertThat(session).isInstanceOf(GemFireSession.class);

		session.setAttribute("userToken", jxblumToken);

		assertThat(session.getId()).isNotNull();
		assertThat(session.getCreationTime()).isBeforeOrEqualTo(Instant.now());
		assertThat(session.isExpired()).isFalse();
		assertThat(session.<UsernamePasswordAuthenticationToken>getAttribute("userToken"))
			.isEqualTo(jxblumToken);

		Session savedSession = save(session);

		assertThat(savedSession).isEqualTo(session);
		assertThat(savedSession).isInstanceOf(GemFireSession.class);

		// NOTE: You must update and save the Session again, after it has already been saved to cause Apache Geode to
		// update the 'principalNameIndex' OQL Index on an Index Maintenance Operation!!!
		((GemFireSession) savedSession).setPrincipalName("pieDoe");

		savedSession = save(touch(savedSession));

		assertThat(savedSession).isEqualTo(session);

		Session loadedSession = get(savedSession.getId());

		assertThat(loadedSession).isEqualTo(savedSession);
		assertThat(loadedSession).isNotSameAs(savedSession);
		// TODO: Problem on Java 17
		//assertThat(loadedSession.getCreationTime()).isEqualTo(savedSession.getCreationTime());
		assertThat(loadedSession.getCreationTime().toEpochMilli()).isEqualTo(savedSession.getCreationTime().toEpochMilli());
		assertThat(loadedSession.getLastAccessedTime()).isAfterOrEqualTo(savedSession.getLastAccessedTime());
		assertThat(loadedSession.isExpired()).isFalse();
		assertThat(session.<UsernamePasswordAuthenticationToken>getAttribute("userToken"))
			.isEqualTo(jxblumToken);
	}

	@ClientCacheApplication(logLevel = GEMFIRE_LOG_LEVEL, subscriptionEnabled = true)
	@EnableGemFireHttpSession(poolName = "DEFAULT")
	static class GemFireClientConfiguration {

		@Bean
		static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		ClientCacheConfigurer clientCachePoolPortConfigurer(@Value("${spring.session.data.gemfire.cache.server.port:"
				+ CacheServer.DEFAULT_PORT + "}") int cacheServerPort) {

			return (beanName, clientCacheFactoryBean) -> clientCacheFactoryBean.setServers(Collections.singletonList(
				new ConnectionEndpoint("localhost", cacheServerPort)));
		}
	}
}
