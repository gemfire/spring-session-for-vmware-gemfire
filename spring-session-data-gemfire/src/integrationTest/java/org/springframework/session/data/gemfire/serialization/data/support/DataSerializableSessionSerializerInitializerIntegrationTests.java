/*
 * Copyright (c) VMware, Inc. 2022. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization.data.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import static org.springframework.session.data.gemfire.serialization.data.support.DataSerializableSessionSerializerInitializer.InitializingGemFireOperationsSessionRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.DataSerializer;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.internal.InternalDataSerializer;

import org.springframework.session.data.gemfire.AbstractGemFireIntegrationTests;
import org.springframework.session.data.gemfire.serialization.data.provider.DataSerializableSessionAttributesSerializer;
import org.springframework.session.data.gemfire.serialization.data.provider.DataSerializableSessionSerializer;

/**
 * Unit tests for {@link DataSerializableSessionSerializerInitializer}.
 *
 * @author John Blum
 * @see Test
 * @see Mock
 * @see org.mockito.Mockito
 * @see org.mockito.Spy
 * @see MockitoJUnitRunner
 * @see DataSerializer
 * @see InternalDataSerializer
 * @see AbstractGemFireIntegrationTests
 * @see DataSerializableSessionAttributesSerializer
 * @see DataSerializableSessionSerializer
 * @see DataSerializableSessionSerializerInitializer
 * @since 2.1.1
 */
@RunWith(MockitoJUnitRunner.class)
public class DataSerializableSessionSerializerInitializerIntegrationTests extends AbstractGemFireIntegrationTests {

	@BeforeClass
	public static void clearUsingDataSerialization() {
		InitializingGemFireOperationsSessionRepository.INSTANCE.setUseDataSerialization(false);
	}

	@Mock
	private Cache mockCache;

	@Test
	public void constructDataSerializableSessionSerializerInitializerWithGemFireCache() {

		DataSerializableSessionSerializerInitializer initializer =
			new DataSerializableSessionSerializerInitializer(this.mockCache);

		assertThat(initializer.getGemFireCache().orElse(null)).isSameAs(this.mockCache);
		assertThat(initializer.getLogger()).isNotNull();
	}

	@Test
	public void constructDataSerializableSessionSerializerInitializerWithNull() {

		DataSerializableSessionSerializerInitializer initializer =
			new DataSerializableSessionSerializerInitializer(null);

		assertThat(initializer.getGemFireCache().orElse(null)).isNull();
		assertThat(initializer.getLogger()).isNotNull();
	}

	@Test
	public void ofFactoryMethodInitializesDataSerializableSessionSerializerInitializer() {

		DataSerializableSessionSerializerInitializer initializer =
			DataSerializableSessionSerializerInitializer.of(this.mockCache);

		assertThat(initializer).isNotNull();
		assertThat(initializer.getGemFireCache().orElse(null)).isSameAs(this.mockCache);
		assertThat(initializer.getLogger()).isNotNull();
	}

	@Test
	public void ofFactoryMethodWithNullCache() {

		DataSerializableSessionSerializerInitializer initializer =
			DataSerializableSessionSerializerInitializer.of(null);

		assertThat(initializer).isNotNull();
		assertThat(initializer.getGemFireCache().orElse(null)).isNull();
		assertThat(initializer.getLogger()).isNotNull();
	}

	@Test
	public void initializeWithCacheAndPropertiesParametersSetsGemFireCacheReferenceAndCallsDoInitialize() {

		DataSerializableSessionSerializerInitializer initializer =
			spy(DataSerializableSessionSerializerInitializer.of(null));

		doNothing().when(initializer).doInitialization();

		assertThat(initializer).isNotNull();
		assertThat(initializer.getGemFireCache().orElse(null)).isNull();

		initializer.initialize(this.mockCache, new Properties());

		assertThat(initializer.getGemFireCache().orElse(null)).isSameAs(this.mockCache);

		verify(initializer, times(1)).doInitialization();
	}

	@Test
	public void doInitializationResolvesGemFireCachesRegistersSessionSerializersAndConfiguresDataSerialization() {

		DataSerializableSessionSerializerInitializer initializer =
			spy(DataSerializableSessionSerializerInitializer.of(null));

		doReturn(this.mockCache).when(initializer).resolveGemFireCache();
		doNothing().when(initializer).registerDataSerializableSessionSerializer();
		doNothing().when(initializer).configureUseDataSerialization();

		initializer.doInitialization();

		verify(initializer, times(1)).resolveGemFireCache();
		verify(initializer, times(1)).registerDataSerializableSessionSerializer();
		verify(initializer, times(1)).configureUseDataSerialization();
	}

	@Test
	public void doInitializationIsCorrect() {

		assertThat(InitializingGemFireOperationsSessionRepository.INSTANCE.isDataSerializationConfigured()).isFalse();

		DataSerializableSessionSerializerInitializer initializer =
			DataSerializableSessionSerializerInitializer.of(this.mockCache);

		assertThat(initializer).isNotNull();
		assertThat(initializer.getGemFireCache().orElse(null)).isSameAs(this.mockCache);

		initializer.doInitialization();

		assertThat(InitializingGemFireOperationsSessionRepository.INSTANCE.isDataSerializationConfigured()).isTrue();

		List<Class> registeredDataSerializerTypes =
			Arrays.stream(nullSafeArray(InternalDataSerializer.getSerializers(), DataSerializer.class))
				.map(Object::getClass)
				.collect(Collectors.toList());

		assertThat(registeredDataSerializerTypes)
			.containsExactly(DataSerializableSessionSerializer.class, DataSerializableSessionAttributesSerializer.class);
	}

	@Test(expected = CacheClosedException.class)
	public void doInitializationWhenGemFireCacheIsNotPresentThrowsCacheClosedException() {

		DataSerializableSessionSerializerInitializer initializer =
			spy(DataSerializableSessionSerializerInitializer.of(null));

		assertThat(initializer).isNotNull();
		assertThat(initializer.getGemFireCache().orElse(null)).isNull();

		try {
			initializer.doInitialization();
		}
		finally {

			assertThat(initializer.getGemFireCache().orElse(null)).isNull();
			assertThat(InternalDataSerializer.getSerializers()).isEmpty();
			assertThat(InitializingGemFireOperationsSessionRepository.INSTANCE.isDataSerializationConfigured()).isFalse();

			verify(initializer, times(1)).resolveGemFireCache();
			verify(initializer, never()).registerDataSerializableSessionSerializer();
			verify(initializer, never()).configureUseDataSerialization();
		}
	}
}
