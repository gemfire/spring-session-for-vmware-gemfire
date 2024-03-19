/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.serialization.data.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.DataInput;
import java.io.DataOutput;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.support.GemfireBeanFactoryLocator;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.DeltaCapableGemFireSession;
import org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.DeltaCapableGemFireSessionAttributes;
import org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSession;
import org.springframework.session.data.gemfire.AbstractGemFireOperationsSessionRepository.GemFireSessionAttributes;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;
import org.springframework.session.data.gemfire.serialization.SessionSerializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link DataSerializerSessionSerializerAdapter}.
 *
 * @author John Blum
 * @see DataInput
 * @see DataOutput
 * @see Test
 * @see org.mockito.Mockito
 * @see Bean
 * @see Configuration
 * @see GemfireBeanFactoryLocator
 * @see Session
 * @see AbstractGemFireIntegrationTests
 * @see SessionSerializer
 * @see DataSerializerSessionSerializerAdapter
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class DataSerializerSessionSerializerAdapterIntegrationTests extends AbstractGemFireIntegrationTests {

	@Autowired
	private DataSerializerSessionSerializerAdapter<Session> dataSerializer;

	@Autowired
	@Qualifier(GemFireHttpSessionConfiguration.SESSION_SERIALIZER_BEAN_ALIAS)
	private SessionSerializer<Session, DataInput, DataOutput> sessionSerializer;

	@Test
	public void constructsAndAutowiresDataSerializerSessionSerializerAdapter() {

		assertThat(this.dataSerializer).isNotNull();
		assertThat(this.sessionSerializer).isNotNull();
		assertThat(this.dataSerializer.getId()).isEqualTo(0xBAC2BAC);
		assertThat(this.dataSerializer.getSupportedClasses())
			.containsExactly(GemFireSession.class, GemFireSessionAttributes.class, DeltaCapableGemFireSession.class,
				DeltaCapableGemFireSessionAttributes.class);
	}

	@Configuration
	@SuppressWarnings("unused")
	static class TestConfiguration {

		@Bean
		GemfireBeanFactoryLocator beanFactoryLocator(BeanFactory beanFactory) {
			return GemfireBeanFactoryLocator.newBeanFactoryLocator(beanFactory, "sessionBeanFactory");
		}

		@Bean
		@SuppressWarnings("rawtypes")
		DataSerializerSessionSerializerAdapter dataSerializer() {
			return new DataSerializerSessionSerializerAdapter();
		}

		@Bean
		@Qualifier(GemFireHttpSessionConfiguration.SESSION_SERIALIZER_BEAN_ALIAS)
		@SuppressWarnings("unchecked")
		SessionSerializer<Session, DataInput, DataOutput> mockSessionSerializer() {
			return mock(SessionSerializer.class, "SessionSerializer");
		}
	}
}
