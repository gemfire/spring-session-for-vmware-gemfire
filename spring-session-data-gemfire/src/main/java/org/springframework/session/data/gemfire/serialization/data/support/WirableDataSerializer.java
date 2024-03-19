/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization.data.support;

import static org.springframework.data.gemfire.support.GemfireBeanFactoryLocator.newBeanFactoryLocator;

import java.util.Optional;

import org.apache.geode.DataSerializer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
import org.springframework.session.data.gemfire.serialization.data.AbstractDataSerializableSessionSerializer;
import org.springframework.stereotype.Component;

/**
 * {@link WirableDataSerializer} is an abstract base class supporting auto-wiring of a non-managed,
 * GemFire/Geode {@link DataSerializer}, Spring {@link Component}.
 *
 * @author John Blum
 * @see DataSerializer
 * @see BeanFactory
 * @see BeanConfigurerSupport
 * @see AbstractDataSerializableSessionSerializer
 * @since 2.0.0
 */
abstract class WirableDataSerializer<T> extends AbstractDataSerializableSessionSerializer<T> {

	protected final void autowire() {

		locateBeanFactory().map(this::newBeanConfigurer).ifPresent(beanConfigurer -> {
			beanConfigurer.configureBean(this);
			beanConfigurer.destroy();
		});
	}

	Optional<BeanFactory> locateBeanFactory() {

		try {
			return Optional.ofNullable(newBeanFactoryLocator().useBeanFactory());
		}
		catch (Exception ignore) {
			return Optional.empty();
		}
	}

	BeanConfigurerSupport newBeanConfigurer(BeanFactory beanFactory) {

		BeanConfigurerSupport beanConfigurer = new BeanConfigurerSupport();

		beanConfigurer.setBeanFactory(beanFactory);
		beanConfigurer.afterPropertiesSet();

		return beanConfigurer;
	}
}
