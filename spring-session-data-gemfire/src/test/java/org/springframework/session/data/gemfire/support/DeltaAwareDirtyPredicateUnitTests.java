/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.apache.geode.Delta;

/**
 * Unit Tests for {@link DeltaAwareDirtyPredicate}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.session.data.gemfire.support.DeltaAwareDirtyPredicate
 * @since 2.1.2
 */
public class DeltaAwareDirtyPredicateUnitTests {

	@Test
	public void isDirtyIsNullSafe() {

		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty(null, null)).isTrue();
		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty("one", null)).isTrue();
		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty(null, "one")).isTrue();
	}

	@Test
	public void isDirtyWithNonDeltaObjectReturnsTrue() {

		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty("one", "one")).isTrue();
		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty("one", "two")).isTrue();
	}

	@Test
	public void isDirtyWithSameDeltaObjectReturnsTrue() {

		Delta mockDelta = mock(Delta.class);

		when(mockDelta.hasDelta()).thenReturn(true);

		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty(mockDelta, mockDelta)).isTrue();

		verify(mockDelta, times(1)).hasDelta();
	}

	@Test
	public void isDirtyWithSameDeltaObjectReturnsFalse() {

		Delta mockDelta = mock(Delta.class);

		when(mockDelta.hasDelta()).thenReturn(false);

		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty(mockDelta, mockDelta)).isFalse();

		verify(mockDelta, times(1)).hasDelta();
	}

	@Test
	public void isDirtyWithStringAndDeltaObjectReturnsTrue() {

		Delta mockDelta = mock(Delta.class);

		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty("one", mockDelta)).isTrue();

		verify(mockDelta, never()).hasDelta();
	}

	@Test
	public void isDirtyWithNullAndDeltaObjectReturnsTrue() {

		Delta mockDelta = mock(Delta.class);

		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty(null, mockDelta)).isTrue();

		verify(mockDelta, never()).hasDelta();
	}

	@Test
	public void isDirtyWithDeltaObjectAndNullReturnsTrue() {

		Delta mockDelta = mock(Delta.class);

		assertThat(DeltaAwareDirtyPredicate.INSTANCE.isDirty(mockDelta, null)).isTrue();

		verify(mockDelta, never()).hasDelta();
	}
}
