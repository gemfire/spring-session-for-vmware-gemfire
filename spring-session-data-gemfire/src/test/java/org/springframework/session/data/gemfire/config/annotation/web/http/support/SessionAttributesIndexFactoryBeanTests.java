/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.config.annotation.web.http.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Test;

import org.apache.geode.cache.query.Index;

import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.session.data.gemfire.config.annotation.web.http.GemFireHttpSessionConfiguration;

/**
 * Unit Tests for {@link SessionAttributesIndexFactoryBean}.
 *
 * @author John Blum
 * @see Test
 * @see org.mockito.Mockito
 * @see Index
 * @see GemFireHttpSessionConfiguration
 * @see SessionAttributesIndexFactoryBean
 * @since 1.3.0
 */
public class SessionAttributesIndexFactoryBeanTests {

	private SessionAttributesIndexFactoryBean indexFactoryBean;

	@Before
	public void setup() {
		this.indexFactoryBean = new SessionAttributesIndexFactoryBean();
	}

	@Test
	public void indexIsNotInitializedWhenNoIndexableSessionAttributesAreConfigured() throws Exception {

		Index mockIndex = mock(Index.class);

		SessionAttributesIndexFactoryBean indexFactoryBean = spy(new SessionAttributesIndexFactoryBean());

		doReturn(mockIndex).when(indexFactoryBean).newIndex();

		indexFactoryBean.afterPropertiesSet();

		assertThat(indexFactoryBean.getObject()).isNull();
		assertThat(indexFactoryBean.getObjectType()).isEqualTo(Index.class);
	}

	@Test
	public void initializesIndexWhenIndexableSessionAttributesAreConfigured() throws Exception {

		Index mockIndex = mock(Index.class);

		SessionAttributesIndexFactoryBean indexFactoryBean = spy(new SessionAttributesIndexFactoryBean());

		doReturn(mockIndex).when(indexFactoryBean).newIndex();

		indexFactoryBean.setIndexableSessionAttributes(ArrayUtils.asArray("one", "two"));
		indexFactoryBean.afterPropertiesSet();

		assertThat(indexFactoryBean.getObject()).isEqualTo(mockIndex);
		assertThat(indexFactoryBean.getObjectType()).isEqualTo(mockIndex.getClass());
	}

	@Test
	public void isSingletonIsTrue() {
		assertThat(this.indexFactoryBean.isSingleton()).isTrue();
	}

	@Test
	public void setAndGetIndexableSessionAttributes() {

		assertThat(this.indexFactoryBean.getIndexableSessionAttributes())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_INDEXABLE_SESSION_ATTRIBUTES);

		assertThat(this.indexFactoryBean.getIndexableSessionAttributesAsGemFireIndexExpression()).isEqualTo("*");

		this.indexFactoryBean.setIndexableSessionAttributes(ArrayUtils.asArray("one", "two", "three"));

		assertThat(this.indexFactoryBean.getIndexableSessionAttributes())
			.isEqualTo(ArrayUtils.asArray("one", "two", "three"));

		assertThat(this.indexFactoryBean.getIndexableSessionAttributesAsGemFireIndexExpression())
			.isEqualTo("'one', 'two', 'three'");

		this.indexFactoryBean.setIndexableSessionAttributes(ArrayUtils.asArray("one"));

		assertThat(this.indexFactoryBean.getIndexableSessionAttributes()).isEqualTo(ArrayUtils.asArray("one"));
		assertThat(this.indexFactoryBean.getIndexableSessionAttributesAsGemFireIndexExpression()).isEqualTo("'one'");

		this.indexFactoryBean.setIndexableSessionAttributes(null);

		assertThat(this.indexFactoryBean.getIndexableSessionAttributes())
			.isEqualTo(GemFireHttpSessionConfiguration.DEFAULT_INDEXABLE_SESSION_ATTRIBUTES);

		assertThat(this.indexFactoryBean.getIndexableSessionAttributesAsGemFireIndexExpression()).isEqualTo("*");
	}

	@Test
	public void setAndGetRegionName() {

		assertThat(this.indexFactoryBean.getRegionName()).isNull();

		this.indexFactoryBean.setRegionName("Example");

		assertThat(this.indexFactoryBean.getRegionName()).isEqualTo("Example");

		this.indexFactoryBean.setRegionName(null);

		assertThat(this.indexFactoryBean.getRegionName()).isNull();
	}
}
