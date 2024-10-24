/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import com.vmware.gemfire.testcontainers.GemFireCluster;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializable;
import org.apache.geode.pdx.PdxWriter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Integration test to test the {@code findByPrincipalName} query method on {@link GemFireOperationsSessionRepository}.
 *
 * @author John Blum
 * @since 1.1.0
 * @see Test
 * @see RunWith
 * @see Region
 * @see QueryService
 * @see PdxSerializable
 * @see SecurityContext
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see GemFireOperationsSessionRepository
 * @see EnableGemFireHttpSession
 * @see DirtiesContext
 * @see ContextConfiguration
 * @see SpringRunner
 * @see WebAppConfiguration
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
@WebAppConfiguration
public class ClientCacheGemFireOperationsSessionRepositoryIntegrationTests extends AbstractGemFireIntegrationTests {

	private static final int MAX_INACTIVE_INTERVAL_IN_SECONDS = 300;

	private static final String GEMFIRE_LOG_LEVEL = "error";
	private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";
	private static final String SPRING_SESSION_GEMFIRE_REGION_NAME = "TestPartitionedSessions";

	SecurityContext context;

	SecurityContext changedContext;

	private static GemFireCluster gemFireCluster;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		gemFireCluster = new GemFireCluster(System.getProperty("spring.test.gemfire.docker.image"), 1, 1)
				.withClasspath(GemFireCluster.ALL_GLOB, System.getProperty("TEST_JAR_PATH"))
				.withGfsh(false, "create region --type=PARTITION --name=" + SPRING_SESSION_GEMFIRE_REGION_NAME);

		gemFireCluster.acceptLicense().start();
	}

	@AfterClass
	public static void teardown() {
		gemFireCluster.close();
	}

	@Before
	public void setup() {

		this.context = SecurityContextHolder.createEmptyContext();

		this.context.setAuthentication(
				new UsernamePasswordAuthenticationToken("username-" + UUID.randomUUID(),
						"na", AuthorityUtils.createAuthorityList("ROLE_USER")));

		this.changedContext = SecurityContextHolder.createEmptyContext();

		this.changedContext.setAuthentication(new UsernamePasswordAuthenticationToken(
				"changedContext-" + UUID.randomUUID(), "na",
				AuthorityUtils.createAuthorityList("ROLE_USER")));

		assertThat(this.gemfireCache).isNotNull();
		assertThat(this.gemfireSessionRepository).isNotNull();
		assertThat(this.gemfireSessionRepository.getMaxInactiveIntervalInSeconds())
			.isEqualTo(MAX_INACTIVE_INTERVAL_IN_SECONDS);

		Region<Object, Session> sessionRegion = this.gemfireCache.getRegion(SPRING_SESSION_GEMFIRE_REGION_NAME);

		assertRegion(sessionRegion, SPRING_SESSION_GEMFIRE_REGION_NAME, DataPolicy.NORMAL);
		assertEntryIdleTimeout(sessionRegion, ExpirationAction.INVALIDATE, MAX_INACTIVE_INTERVAL_IN_SECONDS);
	}

	protected Map<String, Session> doFindByIndexNameAndIndexValue(
			String indexName, String indexValue) {

		return this.gemfireSessionRepository.findByIndexNameAndIndexValue(indexName,
				indexValue);
	}

	protected Map<String, Session> doFindByPrincipalName(String principalName) {
		return doFindByIndexNameAndIndexValue(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principalName);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	protected Map<String, Session> doFindByPrincipalName(String regionName,
			String principalName) {

		try {
			Region<String, Session> region = this.gemfireCache.getRegion(regionName);

			assertThat(region).isNotNull();

			QueryService queryService = region.getRegionService().getQueryService();

			String queryString = String.format(GemFireOperationsSessionRepository.FIND_SESSIONS_BY_PRINCIPAL_NAME_QUERY,
				region.getFullPath());

			Query query = queryService.newQuery(queryString);

			SelectResults<Session> results =
				(SelectResults<Session>) query.execute(principalName);

			Map<String, Session> sessions = new HashMap<>(results.size());

			for (Session session : results.asList()) {
				sessions.put(session.getId(), session);
			}

			return sessions;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected boolean enableQueryDebugging() {
		return true;
	}

	protected Session setAttribute(Session session, String attributeName,
			Object attributeValue) {

		session.setAttribute(attributeName, attributeValue);

		return session;
	}

	@Test
	public void findSessionsByIndexedSessionAttributeNameValues() {

		Session johnBlumSession = save(touch(setAttribute(
			createSession("johnBlum"), "vip", "yes")));
		Session robWinchSession = save(touch(setAttribute(
			createSession("robWinch"), "vip", "yes")));
		Session jonDoeSession = save(touch(setAttribute(
			createSession("jonDoe"), "vip", "no")));
		Session pieDoeSession = save(touch(setAttribute(
			createSession("pieDoe"), "viper", "true")));
		Session sourDoeSession = save(touch(createSession("sourDoe")));

		assertThat(this.<Session>get(johnBlumSession.getId())).isEqualTo(johnBlumSession);
		assertThat(johnBlumSession.<String>getAttribute("vip")).isEqualTo("yes");
		assertThat(this.<Session>get(robWinchSession.getId())).isEqualTo(robWinchSession);
		assertThat(robWinchSession.<String>getAttribute("vip")).isEqualTo("yes");
		assertThat(this.<Session>get(jonDoeSession.getId())).isEqualTo(jonDoeSession);
		assertThat(jonDoeSession.<String>getAttribute("vip")).isEqualTo("no");
		assertThat(this.<Session>get(pieDoeSession.getId())).isEqualTo(pieDoeSession);
		assertThat(pieDoeSession.getAttributeNames().contains("vip")).isFalse();
		assertThat(this.<Session>get(sourDoeSession.getId())).isEqualTo(sourDoeSession);
		assertThat(sourDoeSession.getAttributeNames().contains("vip")).isFalse();

		Map<String, Session> vipSessions = doFindByIndexNameAndIndexValue("vip", "yes");

		assertThat(vipSessions).isNotNull();
		assertThat(vipSessions.size()).isEqualTo(2);
		assertThat(vipSessions.get(johnBlumSession.getId())).isEqualTo(johnBlumSession);
		assertThat(vipSessions.get(robWinchSession.getId())).isEqualTo(robWinchSession);
		assertThat(vipSessions.containsKey(jonDoeSession.getId()));
		assertThat(vipSessions.containsKey(pieDoeSession.getId()));
		assertThat(vipSessions.containsKey(sourDoeSession.getId()));

		Map<String, Session> nonVipSessions = doFindByIndexNameAndIndexValue(
				"vip", "no");

		assertThat(nonVipSessions).isNotNull();
		assertThat(nonVipSessions.size()).isEqualTo(1);
		assertThat(nonVipSessions.get(jonDoeSession.getId())).isEqualTo(jonDoeSession);
		assertThat(nonVipSessions.containsKey(johnBlumSession.getId()));
		assertThat(nonVipSessions.containsKey(robWinchSession.getId()));
		assertThat(nonVipSessions.containsKey(pieDoeSession.getId()));
		assertThat(nonVipSessions.containsKey(sourDoeSession.getId()));

		Map<String, Session> noSessions = doFindByIndexNameAndIndexValue(
				"nonExistingAttribute", "test");

		assertThat(noSessions).isNotNull();
		assertThat(noSessions.isEmpty()).isTrue();
	}

	@Test
	public void findSessionsByPrincipalName() {

		Session sessionOne = save(touch(createSession("robWinch")));
		Session sessionTwo = save(touch(createSession("johnBlum")));
		Session sessionThree = save(touch(createSession("robWinch")));
		Session sessionFour = save(touch(createSession("johnBlum")));
		Session sessionFive = save(touch(createSession("robWinch")));

		assertThat(this.<Session>get(sessionOne.getId())).isEqualTo(sessionOne);
		assertThat(this.<Session>get(sessionTwo.getId())).isEqualTo(sessionTwo);
		assertThat(this.<Session>get(sessionThree.getId())).isEqualTo(sessionThree);
		assertThat(this.<Session>get(sessionFour.getId())).isEqualTo(sessionFour);
		assertThat(this.<Session>get(sessionFive.getId())).isEqualTo(sessionFive);

		Map<String, Session> johnBlumSessions = doFindByPrincipalName("johnBlum");

		assertThat(johnBlumSessions).isNotNull();
		assertThat(johnBlumSessions.size()).isEqualTo(2);
		assertThat(johnBlumSessions.containsKey(sessionOne.getId())).isFalse();
		assertThat(johnBlumSessions.containsKey(sessionThree.getId())).isFalse();
		assertThat(johnBlumSessions.containsKey(sessionFive.getId())).isFalse();
		assertThat(johnBlumSessions.get(sessionTwo.getId())).isEqualTo(sessionTwo);
		assertThat(johnBlumSessions.get(sessionFour.getId())).isEqualTo(sessionFour);

		Map<String, Session> robWinchSessions = doFindByPrincipalName("robWinch");

		assertThat(robWinchSessions).isNotNull();
		assertThat(robWinchSessions.size()).isEqualTo(3);
		assertThat(robWinchSessions.containsKey(sessionTwo.getId())).isFalse();
		assertThat(robWinchSessions.containsKey(sessionFour.getId())).isFalse();
		assertThat(robWinchSessions.get(sessionOne.getId())).isEqualTo(sessionOne);
		assertThat(robWinchSessions.get(sessionThree.getId())).isEqualTo(sessionThree);
		assertThat(robWinchSessions.get(sessionFive.getId())).isEqualTo(sessionFive);
	}

	@Test
	public void findSessionsBySecurityPrincipalName() {

		Session toSave = this.gemfireSessionRepository.createSession();

		toSave.setAttribute(SPRING_SECURITY_CONTEXT, this.context);

		save(toSave);

		Map<String, Session> findByPrincipalName = doFindByPrincipalName(getSecurityName());

		assertThat(findByPrincipalName).hasSize(1);
		assertThat(findByPrincipalName.keySet()).containsOnly(toSave.getId());
	}

	@Test
	public void findSessionsByChangedSecurityPrincipalName() {

		Session toSave = this.gemfireSessionRepository.createSession();

		toSave.setAttribute(SPRING_SECURITY_CONTEXT, this.context);

		save(toSave);

		toSave.setAttribute(SPRING_SECURITY_CONTEXT, this.changedContext);

		save(toSave);

		Map<String, Session> findByPrincipalName = doFindByPrincipalName(getSecurityName());

		assertThat(findByPrincipalName).isEmpty();

		findByPrincipalName = doFindByPrincipalName(getChangedSecurityName());

		assertThat(findByPrincipalName).hasSize(1);
	}

	@Test
	public void findsNoSessionsByNonExistingPrincipal() {

		Map<String, Session> nonExistingPrincipalSessions = doFindByPrincipalName("nonExistingPrincipalName");

		assertThat(nonExistingPrincipalSessions).isNotNull();
		assertThat(nonExistingPrincipalSessions.isEmpty()).isTrue();
	}

	@Test
	public void findsNoSessionsAfterPrincipalIsRemoved() {

		String username = "doesNotFindAfterPrincipalRemoved";

		Session session = save(touch(createSession(username)));

		session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, null);

		save(session);

		Map<String, Session> nonExistingPrincipalSessions = doFindByPrincipalName(username);

		assertThat(nonExistingPrincipalSessions).isNotNull();
		assertThat(nonExistingPrincipalSessions.isEmpty()).isTrue();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void saveAndReadSessionWithAttributes() {

		Session expectedSession = this.gemfireSessionRepository.createSession();

		assertThat(expectedSession).isInstanceOf(AbstractGemFireOperationsSessionRepository.GemFireSession.class);

		((AbstractGemFireOperationsSessionRepository.GemFireSession) expectedSession).setPrincipalName("jblum");

		List<String> expectedAttributeNames =
			Arrays.asList("booleanAttribute", "numericAttribute", "stringAttribute", "personAttribute");

		Person jonDoe = new Person("Jon", "Doe");

		expectedSession.setAttribute(expectedAttributeNames.get(0), true);
		expectedSession.setAttribute(expectedAttributeNames.get(1), Math.PI);
		expectedSession.setAttribute(expectedAttributeNames.get(2), "test");
		expectedSession.setAttribute(expectedAttributeNames.get(3), jonDoe);

		this.gemfireSessionRepository.save(touch(expectedSession));

		Session savedSession = this.gemfireSessionRepository.findById(expectedSession.getId());

		assertThat(savedSession).isEqualTo(expectedSession);
		assertThat(savedSession).isInstanceOf(AbstractGemFireOperationsSessionRepository.GemFireSession.class);
		assertThat(((AbstractGemFireOperationsSessionRepository.GemFireSession) savedSession).getPrincipalName())
			.isEqualTo("jblum");

		assertThat(savedSession.getAttributeNames().containsAll(expectedAttributeNames))
			.as(String.format("Expected (%1$s); but was (%2$s)", expectedAttributeNames, savedSession.getAttributeNames()))
				.isTrue();

		assertThat(savedSession.<Boolean>getAttribute(expectedAttributeNames.get(0))).isTrue();
		assertThat(savedSession.<Double>getAttribute(expectedAttributeNames.get(1))).isEqualTo(Math.PI);
		assertThat(savedSession.<String>getAttribute(expectedAttributeNames.get(2))).isEqualTo("test");
		assertThat(savedSession.<Person>getAttribute(expectedAttributeNames.get(3))).isEqualTo(jonDoe);
	}

	private String getSecurityName() {
		return this.context.getAuthentication().getName();
	}

	private String getChangedSecurityName() {
		return this.changedContext.getAuthentication().getName();
	}

	@ClientCacheApplication(name = "ClientCacheGemFireOperationsSessionRepositoryIntegrationTests", logLevel = GEMFIRE_LOG_LEVEL)
	@EnableGemFireHttpSession(regionName = SPRING_SESSION_GEMFIRE_REGION_NAME,
		maxInactiveIntervalInSeconds = MAX_INACTIVE_INTERVAL_IN_SECONDS,
	clientRegionShortcut = ClientRegionShortcut.LOCAL)
	static class SpringSessionGemFireConfiguration {
		@Bean
		PoolFactoryBean gemfirePool() {
			PoolFactoryBean poolFactory = new PoolFactoryBean();
			poolFactory.addLocators(new ConnectionEndpoint("localhost", gemFireCluster.getLocatorPort()));
			return poolFactory;
		}
	}

	public static class Person implements Comparable<Person>, PdxSerializable {

		private String firstName;
		private String lastName;

		public Person() {
		}

		public Person(String firstName, String lastName) {
			this.firstName = validate(firstName);
			this.lastName = validate(lastName);
		}

		private String validate(String value) {
			Assert.hasText(value, String.format("Value [%s] is required", value));
			return value;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public String getName() {
			return String.format("%1$s %2$s", getFirstName(), getLastName());
		}

		public void toData(PdxWriter pdxWriter) {
			pdxWriter.writeString("firstName", getFirstName());
			pdxWriter.writeString("lastName", getLastName());
		}

		public void fromData(PdxReader pdxReader) {
			this.firstName = pdxReader.readString("firstName");
			this.lastName = pdxReader.readString("lastName");
		}

		public int compareTo(Person person) {

			int compareValue = getLastName().compareTo(person.getLastName());

			return (compareValue != 0 ? compareValue : getFirstName().compareTo(person.getFirstName()));
		}

		@Override
		public boolean equals(Object obj) {

			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Person)) {
				return false;
			}

			Person that = (Person) obj;

			return ObjectUtils.nullSafeEquals(this.getFirstName(), that.getFirstName())
				&& ObjectUtils.nullSafeEquals(this.getLastName(), that.getLastName());
		}

		@Override
		public int hashCode() {

			int hashValue = 17;

			hashValue = 37 * hashValue + ObjectUtils.nullSafeHashCode(getFirstName());
			hashValue = 37 * hashValue + ObjectUtils.nullSafeHashCode(getLastName());

			return hashValue;
		}

		@Override
		public String toString() {
			return getName();
		}
	}
}
