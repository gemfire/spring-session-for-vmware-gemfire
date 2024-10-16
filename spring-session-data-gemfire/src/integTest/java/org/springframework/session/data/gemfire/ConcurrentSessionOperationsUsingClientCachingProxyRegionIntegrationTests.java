/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;
import static org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSession;
import com.vmware.gemfire.testcontainers.GemFireCluster;
import edu.umd.cs.mtc.TestFramework;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.geode.DataSerializer;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.internal.InternalDataSerializer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the concurrent access of a {@link Session} stored in an Apache Geode
 * {@link ClientCache client} {@link ClientRegionShortcut#LOCAL} {@link Region}.
 *
 * @author John Blum
 * @see Instant
 * @see Test
 * @see Mockito
 * @see DataSerializer
 * @see ClientCache
 * @see Region
 * @see ClientCache
 * @see Pool
 * @see PoolManager
 * @see Session
 * @see AbstractConcurrentSessionOperationsIntegrationTests
 * @see SessionSerializer
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.x
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = ConcurrentSessionOperationsUsingClientCachingProxyRegionIntegrationTests.GemFireClientConfiguration.class
)
@SuppressWarnings("unused")
public class ConcurrentSessionOperationsUsingClientCachingProxyRegionIntegrationTests
		extends AbstractConcurrentSessionOperationsIntegrationTests {

	private static GemFireCluster gemFireCluster;

	@Before
	public void setup() {

		ClientCache cache = getGemFireCache();

		assertThat(cache).isNotNull();
		assertThat(cache.getCopyOnRead()).isFalse();
		assertThat(cache.getPdxSerializer()).isNull();
		assertThat(cache.getPdxReadSerialized()).isFalse();

		Pool defaultPool = PoolManager.find("DEFAULT");

		assertThat(defaultPool).isNotNull();
		assertThat(defaultPool.getSubscriptionEnabled()).isTrue();

		Region<Object, Session> sessions = cache.getRegion("Sessions");

		assertThat(sessions).isNotNull();
		assertThat(sessions.getName()).isEqualTo("Sessions");
		assertThat(sessions.getAttributes()).isNotNull();
		assertThat(sessions.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
	}

	@Test
	public void concurrentCachedSessionOperationsAreCorrect() throws Throwable {
		TestFramework.runOnce(new ConcurrentCachedSessionOperationsTestCase(this));
	}

	@Test
	public void regionPutWithNonDirtySessionResultsInInefficientIncorrectBehavior() throws Throwable {
		TestFramework.runOnce(new RegionPutWithNonDirtySessionTestCase(this));
	}

	// Tests that 2 Threads share the same Session object reference and therefore see's each other's changes.
	public static class ConcurrentCachedSessionOperationsTestCase extends AbstractConcurrentSessionOperationsTestCase {

		public ConcurrentCachedSessionOperationsTestCase(
				@NonNull ConcurrentSessionOperationsUsingClientCachingProxyRegionIntegrationTests testInstance) {

			super(testInstance);
		}

		@Override
		public void initialize() {

			Instant beforeCreationTime = Instant.now();

			Session session = newSession();

			assertThat(session).isNotNull();
			assertThat(session.getId()).isNotEmpty();
			assertThat(session.getCreationTime()).isAfterOrEqualTo(beforeCreationTime);
			assertThat(session.getCreationTime()).isBeforeOrEqualTo(Instant.now());
			assertThat(session.getLastAccessedTime()).isEqualTo(session.getCreationTime());
			assertThat(session.isExpired()).isFalse();
			assertThat(session.getAttributeNames()).isEmpty();

			save(session);

			setSessionId(session.getId());
		}

		public void thread1() {

			Thread.currentThread().setName("User Session One");

			assertTick(0);

			Instant beforeLastAccessedTime = Instant.now();

			Session session = findById(requireSessionId());

			assertThat(session).isNotNull();
			assertThat(session.getId()).isEqualTo(getSessionId());
			assertThat(session.getLastAccessedTime()).isAfterOrEqualTo(beforeLastAccessedTime);
			assertThat(session.getLastAccessedTime()).isBeforeOrEqualTo(Instant.now());
			assertThat(session.isExpired()).isFalse();
			assertThat(session.getAttributeNames()).isEmpty();

			save(session);

			waitForTick(2);
			assertTick(2);

			// modify the Session without saving
			session.setAttribute("attributeOne", "one");
			session.setAttribute("attributeTwo", "two");
		}

		public void thread2() {

			Thread.currentThread().setName("User Session Two");

			waitForTick(1);
			assertTick(1);

			Instant beforeLastAccessedTime = Instant.now();

			Session session = findById(requireSessionId());

			assertThat(session).isNotNull();
			assertThat(session.getId()).isEqualTo(getSessionId());
			assertThat(session.getLastAccessedTime()).isAfterOrEqualTo(beforeLastAccessedTime);
			assertThat(session.getLastAccessedTime()).isBeforeOrEqualTo(Instant.now());
			assertThat(session.isExpired()).isFalse();
			assertThat(session.getAttributeNames()).isEmpty();

			waitForTick(3);
			assertTick(3);

			session = findById(requireSessionId());

			assertThat(session.getAttributeNames()).containsOnly("attributeOne", "attributeTwo");
			assertThat(session.<String>getAttribute("attributeOne")).isEqualTo("one");
			assertThat(session.<String>getAttribute("attributeTwo")).isEqualTo("two");
		}
	}

	// Tests that DataSerializer.toData(..) is called twice; once for when the Session is new
	// and again when Region.put(..) is called with a Session having no delta/no changes.
	public static class RegionPutWithNonDirtySessionTestCase extends AbstractConcurrentSessionOperationsTestCase {

		private static final String DATA_SERIALIZER_NOT_FOUND_EXCEPTION_MESSAGE =
			"No DataSerializer was found capable of de/serializing Sessions";

		private final DataSerializer sessionSerializer;

		private final Region<Object, Session> sessions;

		public RegionPutWithNonDirtySessionTestCase(
				@NonNull ConcurrentSessionOperationsUsingClientCachingProxyRegionIntegrationTests testInstance) {

			super(testInstance);

			this.sessions = testInstance.getSessionRegion();
			this.sessionSerializer = reregisterDataSerializer(resolveDataSerializer());
		}

		private @NonNull DataSerializer reregisterDataSerializer(@NonNull DataSerializer dataSerializer) {

			InternalDataSerializer.unregister(dataSerializer.getId());
			InternalDataSerializer._register(dataSerializer, false);

			return dataSerializer;
		}

		private @NonNull DataSerializer resolveDataSerializer() {

			return Arrays.stream(nullSafeArray(InternalDataSerializer.getSerializers(), DataSerializer.class))
				.filter(this.sessionSerializerFilter())
				.findFirst()
				.map(Mockito::spy)
				.orElseThrow(() -> newIllegalStateException(DATA_SERIALIZER_NOT_FOUND_EXCEPTION_MESSAGE));
		}

		private @NonNull Predicate<? super DataSerializer> sessionSerializerFilter() {

			return dataSerializer -> {

				boolean isSessionSerializer = dataSerializer instanceof SessionSerializer;

				if (!isSessionSerializer) {
					isSessionSerializer =
						Arrays.stream(nullSafeArray(dataSerializer.getSupportedClasses(), Class.class))
							.filter(Objects::nonNull)
							.anyMatch(Session.class::isAssignableFrom);
				}

				return isSessionSerializer;
			};
		}

		private @Nullable Session get(@NonNull String id) {
			return this.sessions.get(id);
		}

		private void put(@NonNull Session session) {

			this.sessions.put(session.getId(), session);

			if (session instanceof GemFireSession) {
				((GemFireSession<?>) session).commit();
			}
		}

		public void thread1() {

			Thread.currentThread().setName("User Session One");

			assertTick(0);

			Session session = newSession();

			assertThat(session).isInstanceOf(GemFireSession.class);
			assertThat(session.getId()).isNotEmpty();
			assertThat(session.isExpired()).isFalse();
			assertThat(session.getAttributeNames()).isEmpty();

			session.setAttribute("attributeOne", "testOne");
			session.setAttribute("attributeTwo", "testTwo");

			assertThat(session.getAttributeNames()).containsOnly("attributeOne", "attributeTwo");
			assertThat(((GemFireSession<?>) session).hasDelta()).isTrue();

			put(session);

			assertThat(((GemFireSession<?>) session).hasDelta()).isFalse();

			// Reload to (fully) deserialize Session
			Session loadedSession = get(session.getId());

			assertThat(loadedSession).isInstanceOf(GemFireSession.class);
			assertThat(loadedSession.getId()).isEqualTo(session.getId());

			getSessionRepository().commit(loadedSession);

			setSessionId(session.getId());
		}

		public void thread2() {

			Thread.currentThread().setName("User Session Two");

			waitForTick(1);
			assertTick(1);
			waitOnRequiredSessionId();

			Session session = get(requireSessionId());

			assertThat(session).isInstanceOf(GemFireSession.class);
			assertThat(session.getId()).isEqualTo(getSessionId());
			assertThat(session.isExpired()).isFalse();
			assertThat(session.getAttributeNames()).containsOnly("attributeOne", "attributeTwo");
			assertThat(session.<String>getAttribute("attributeOne")).isEqualTo("testOne");
			assertThat(session.<String>getAttribute("attributeTwo")).isEqualTo("testTwo");
			assertThat(((GemFireSession<?>) session).hasDelta()).isFalse();

			put(session);
		}

		@Override
		public void finish() {

			Session session = get(getSessionId());

			assertThat(session).isNotNull();
			assertThat(session.getId()).isEqualTo(getSessionId());
			assertThat(session.isExpired()).isFalse();
			assertThat(session.getAttributeNames()).containsOnly("attributeOne", "attributeTwo");
			assertThat(session.<String>getAttribute("attributeOne")).isEqualTo("testOne");
			assertThat(session.<String>getAttribute("attributeTwo")).isEqualTo("testTwo");

			try {

				// The first Region.get(key) causes a deserialization (???)
				verify(this.sessionSerializer, never()).fromData(any(DataInput.class));

				verify(this.sessionSerializer, times(2))
					.toData(isA(GemFireSession.class), isA(DataOutput.class));

			}
			catch (ClassNotFoundException | IOException ignore) { }
		}
	}

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

	// Tests fail when 'copyOnRead' is set to 'true'!
	//@ClientCacheApplication(copyOnRead = true, logLevel = GEMFIRE_LOG_LEVEL, subscriptionEnabled = true)
	@ClientCacheApplication(subscriptionEnabled = true)
	@EnableGemFireHttpSession(
		clientRegionShortcut = ClientRegionShortcut.CACHING_PROXY,
		poolName = "DEFAULT",
		regionName = "Sessions",
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	static class GemFireClientConfiguration { }
}
