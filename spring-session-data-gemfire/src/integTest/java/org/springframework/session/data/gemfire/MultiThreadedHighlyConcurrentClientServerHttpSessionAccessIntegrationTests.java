/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.vmware.gemfire.testcontainers.GemFireCluster;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.tests.util.IdentityHashCodeComparator;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Multi-Threaded, Highly-Concurrent, {@link Session} Data Access Operations Integration Test.
 *
 * @author John Blum
 * @see ExecutorService
 * @see Test
 * @see ClientCacheApplication
 * @see Session
 * @see org.springframework.session.SessionRepository
 * @see EnableGemFireHttpSession
 * @see org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.2
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = MultiThreadedHighlyConcurrentClientServerHttpSessionAccessIntegrationTests.GemFireClientConfiguration.class
)
@SuppressWarnings("unused")
public class MultiThreadedHighlyConcurrentClientServerHttpSessionAccessIntegrationTests
		extends AbstractGemFireIntegrationTests {

	private static final boolean SESSION_REFERENCE_CHECKING_ENABLED = false;

	// TODO: Set WORKLOAD_SIZE back to 10,000 once Apache Geode fixes its concurrency and resource problems!
	//  NOTE: This issue may be related to (from Anil Gingade): https://issues.apache.org/jira/browse/GEODE-7663
	//  NOTE: See https://issues.apache.org/jira/browse/GEODE-7763
	private static final int THREAD_COUNT = 180;
	private static final int WORKLOAD_SIZE = 3000;

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withCacheXml(GemFireCluster.ALL_GLOB, "/session-serializer-cache.xml")
				.withClasspath(GemFireCluster.ALL_GLOB, System.getProperty("TEST_JAR_PATH"))
				.withGfsh(false, "create region --type=PARTITION --name=Sessions");

		gemFireCluster.acceptLicense().start();

		System.setProperty("spring.data.gemfire.pool.locators", String.format("localhost[%d]", gemFireCluster.getLocatorPort()));
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	private final AtomicInteger sessionReferenceComparisonCounter = new AtomicInteger(0);
	private final AtomicInteger threadCounter = new AtomicInteger(0);

	private final AtomicReference<String> sessionId = new AtomicReference<>(null);

	private final List<String> sessionAttributeNames = Collections.synchronizedList(new ArrayList<>(WORKLOAD_SIZE));

	private final Random random = new Random(System.currentTimeMillis());

	private final Set<Session> sessionIdentityHashCodes =
		Collections.synchronizedSet(new TreeSet<>(IdentityHashCodeComparator.INSTANCE));

	private final Set<Session> sessionReferences =
		Collections.synchronizedSet(new TreeSet<>((sessionOne, sessionTwo) -> sessionOne == sessionTwo ? 0
			: this.sessionReferenceComparisonCounter.incrementAndGet() % 2 == 0 ? -1 : 1));

	@Before
	public void assertGemFireConfiguration() {

		assertThat(this.gemfireCache).isNotNull();

		assertThat(this.gemfireCache.getPdxSerializer())
			.describedAs("Expected the configured PdxSerializer to be null; but was [%s]",
				ObjectUtils.nullSafeClassName(this.gemfireCache.getPdxSerializer()))
			.isNull();

		assertThat(this.sessions).isNotNull();
		assertThat(this.sessions.getAttributes()).isNotNull();

		assertThat(this.sessions.getAttributes().getDataPolicy())
			.describedAs("Expected Region [%s] DataPolicy of EMPTY; but was %s",
				this.sessions.getName(), this.sessions.getAttributes().getDataPolicy())
			.isEqualTo(DataPolicy.EMPTY);
	}

	@Before
	public void setupSession() {

		Instant beforeCreationTime = Instant.now();

		Session session = createSession();

		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEmpty();
		assertThat(session.getCreationTime()).isAfterOrEqualTo(beforeCreationTime);
		assertThat(session.getCreationTime()).isBeforeOrEqualTo(Instant.now());
		assertThat(session.getLastAccessedTime()).isEqualTo(session.getCreationTime());
		assertThat(session.isExpired()).isFalse();
		assertThat(session.getAttributeNames()).isEmpty();

		this.sessionId.set(save(touch(session)).getId());
	}

	private void assertUniqueSessionReference(Session session) {

		if (SESSION_REFERENCE_CHECKING_ENABLED) {
			assertThat(this.sessionReferences.add(session))
				.describedAs("Session reference was not unique; size [%d]", this.sessionReferences.size())
				.isTrue();
		}
	}

	private ExecutorService newSessionWorkloadExecutor() {

		return Executors.newFixedThreadPool(THREAD_COUNT, runnable -> {

			Thread sessionThread = new Thread(runnable);

			sessionThread.setDaemon(true);
			sessionThread.setName(String.format("Session Thread %d", this.threadCounter.incrementAndGet()));
			sessionThread.setPriority(Thread.NORM_PRIORITY);

			return sessionThread;
		});
	}

	private Collection<Callable<Integer>> newSessionWorkloadTasks() {

		Collection<Callable<Integer>> sessionWorkloadTasks = new ArrayList<>(WORKLOAD_SIZE);

		for (int count = 0, readCount = 0; count < WORKLOAD_SIZE; count++, readCount = 3 * count) {

			sessionWorkloadTasks.add(count % 79 != 0
				? newAddSessionAttributeTask()
				: readCount % 237 != 0
				? newRemoveSessionAttributeTask()
				: newSessionReaderTask());
		}

		return sessionWorkloadTasks;
	}

	private Callable<Integer> newAddSessionAttributeTask() {

		return () -> {

			Instant beforeLastAccessedTime = Instant.now();

			Session session = get(this.sessionId.get());

			assertSession(session, beforeLastAccessedTime);

			String attributeName = UUID.randomUUID().toString();
			Object attributeValue = System.currentTimeMillis();

			session.setAttribute(attributeName, attributeValue);

			save(touch(session));

			this.sessionAttributeNames.add(attributeName);

			return 1;
		};
	}

	private void assertSession(Session session, Instant beforeLastAccessedTime) {
		assertThat(session).isNotNull();
		assertThat(session.getId()).isEqualTo(this.sessionId.get());
		assertThat(session.getLastAccessedTime()).isAfterOrEqualTo(beforeLastAccessedTime);
		assertThat(session.getLastAccessedTime()).isBeforeOrEqualTo(Instant.now());
		assertThat(session.isExpired()).isFalse();
		assertUniqueSessionReference(session);
	}

	private Callable<Integer> newRemoveSessionAttributeTask() {

		return () -> {

			int returnValue = 0;

			Instant beforeLastAccessedTime = Instant.now();

			Session session = get(this.sessionId.get());

			assertSession(session, beforeLastAccessedTime);

			String attributeName = null;

			synchronized (this.sessionAttributeNames) {

				int size = this.sessionAttributeNames.size();

				if (size > 0) {

					int index = this.random.nextInt(size);

					attributeName = this.sessionAttributeNames.remove(index);
				}
			}

			if (session.getAttributeNames().contains(attributeName)) {
				session.removeAttribute(attributeName);
				returnValue = -1;
			}
			else if (StringUtils.hasText(attributeName)){
				this.sessionAttributeNames.add(attributeName);
			}

			save(touch(session));

			return returnValue;
		};
	}

	private Callable<Integer> newSessionReaderTask() {

		return () -> {

			Instant beforeLastAccessedTime = Instant.now();

			Session session = get(this.sessionId.get());

			assertSession(session, beforeLastAccessedTime);

			save(session);

			return 0;
		};
	}

	private <T> T safeFutureGet(Future<T> future) {

		try {
			return future.get();
		}
		catch (Exception cause) {
			throw new RuntimeException("Session Access Task Failed", cause);
		}
	}

	private int runSessionWorkload() throws InterruptedException {

		ExecutorService sessionWorkloadExecutor = newSessionWorkloadExecutor();

		try {

			List<Future<Integer>> sessionWorkloadTasksFutures =
				sessionWorkloadExecutor.invokeAll(newSessionWorkloadTasks());

			return sessionWorkloadTasksFutures.stream()
				.mapToInt(this::safeFutureGet)
				.sum();
		}
		finally {
			sessionWorkloadExecutor.shutdownNow();
		}
	}

	@Test
	public void concurrentSessionAccessIsCorrect() throws InterruptedException {

		int sessionAttributeCount = runSessionWorkload();

		assertThat(sessionAttributeCount).isEqualTo(this.sessionAttributeNames.size());

		Session session = get(this.sessionId.get());

		assertThat(session).isNotNull();
		assertThat(session.getId()).isEqualTo(this.sessionId.get());
		assertThat(session.getAttributeNames()).hasSize(sessionAttributeCount);
	}

	@ClientCacheApplication(subscriptionEnabled = true)
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.PROXY,
		poolName = "DEFAULT",
		regionName = "Sessions",
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	static class GemFireClientConfiguration { }
}
