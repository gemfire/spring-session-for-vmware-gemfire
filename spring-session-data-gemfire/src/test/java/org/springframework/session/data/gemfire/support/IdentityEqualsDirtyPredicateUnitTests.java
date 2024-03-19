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

import org.junit.Test;

/**
 * Unit Tests for {@link IdentityEqualsDirtyPredicate}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.session.data.gemfire.support.IdentityEqualsDirtyPredicate
 * @since 2.1.2
 */
public class IdentityEqualsDirtyPredicateUnitTests {

	@Test
	public void isDirtyIsNullSafe() {

		assertThat(IdentityEqualsDirtyPredicate.INSTANCE.isDirty(null, null)).isFalse();
		assertThat(IdentityEqualsDirtyPredicate.INSTANCE.isDirty("one", null)).isTrue();
		assertThat(IdentityEqualsDirtyPredicate.INSTANCE.isDirty(null, "one")).isTrue();
	}

	@Test
	public void isDirtyWithDifferentObjectsReturnsTrue() {
		assertThat(IdentityEqualsDirtyPredicate.INSTANCE.isDirty("one", "two")).isTrue();
	}

	@Test
	public void isDirtyWithSameObjectsReturnsFalse() {
		assertThat(IdentityEqualsDirtyPredicate.INSTANCE.isDirty("one", "one")).isFalse();
	}
}
