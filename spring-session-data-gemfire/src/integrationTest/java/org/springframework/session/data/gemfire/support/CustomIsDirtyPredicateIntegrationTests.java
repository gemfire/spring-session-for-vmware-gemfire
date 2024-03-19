/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.GemFireOperationsSessionRepository;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for custom {@link IsDirtyPredicate}.
 *
 * @author John Blum
 * @see Test
 * @see IsDirtyPredicate
 * @see AbstractGemFireIntegrationTests
 * @since 2.1.2
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class CustomIsDirtyPredicateIntegrationTests extends AbstractGemFireIntegrationTests {

	@Before
	public void setup() {

		GemFireOperationsSessionRepository sessionRepository = getSessionRepository();

		assertThat(sessionRepository).isNotNull();
		assertThat(sessionRepository.getIsDirtyPredicate())
			.isInstanceOf(OddNumberDirtyPredicateStrategy.class);
	}

	@Test
	public void isDirtyStrategyIsCorrect() {

		GemFireSession<?> session = commit(createSession());

		assertThat(session).isNotNull();
		assertThat(session.hasDelta()).isFalse();

		session.setAttribute("1", 2);

		assertThat(session.getAttributeNames()).containsExactly("1");
		assertThat(session.<Integer>getAttribute("1")).isEqualTo(2);
		assertThat(session.hasDelta()).isFalse();

		session.setAttribute("2", 1);

		assertThat(session.getAttributeNames()).containsOnly("1", "2");
		assertThat(session.<Integer>getAttribute("1")).isEqualTo(2);
		assertThat(session.<Integer>getAttribute("2")).isEqualTo(1);
		assertThat(session.hasDelta()).isTrue();

		commit(session);

		assertThat(session.getAttributeNames()).containsOnly("1", "2");
		assertThat(session.hasDelta()).isFalse();

		session.setAttribute("2", 1);

		assertThat(session.getAttributeNames()).containsOnly("1", "2");
		assertThat(session.<Integer>getAttribute("1")).isEqualTo(2);
		assertThat(session.<Integer>getAttribute("2")).isEqualTo(1);
		assertThat(session.hasDelta()).isTrue();

		commit(session);

		assertThat(session.getAttributeNames()).containsOnly("1", "2");
		assertThat(session.hasDelta()).isFalse();

		session.setAttribute("3", 4);

		assertThat(session.getAttributeNames()).containsOnly("1", "2", "3");
		assertThat(session.<Integer>getAttribute("1")).isEqualTo(2);
		assertThat(session.<Integer>getAttribute("2")).isEqualTo(1);
		assertThat(session.<Integer>getAttribute("3")).isEqualTo(4);
		assertThat(session.hasDelta()).isFalse();

		session.setAttribute("2", 3);

		assertThat(session.getAttributeNames()).containsOnly("1", "2", "3");
		assertThat(session.<Integer>getAttribute("1")).isEqualTo(2);
		assertThat(session.<Integer>getAttribute("2")).isEqualTo(3);
		assertThat(session.<Integer>getAttribute("3")).isEqualTo(4);
		assertThat(session.hasDelta()).isTrue();
	}

	@ClientCacheApplication
	@EnableGemFireMockObjects
	@EnableGemFireHttpSession(
		poolName = "DEFAULT",
		sessionSerializerBeanName = GemFireHttpSessionConfiguration.SESSION_DATA_SERIALIZER_BEAN_NAME
	)
	@SuppressWarnings("unused")
	static class TestConfiguration {

		@Bean
		IsDirtyPredicate oddNumberDirtyPredicate() {
			return new OddNumberDirtyPredicateStrategy();
		}
	}

	static class OddNumberDirtyPredicateStrategy implements IsDirtyPredicate {

		@Override
		public boolean isDirty(Object oldValue, Object newValue) {
			return toInteger(newValue) % 2 != 0;
		}

		private int toInteger(Object value) {

			return value instanceof Number
				? ((Number) value).intValue()
				: Integer.parseInt(String.valueOf(value));
		}
	}
}
