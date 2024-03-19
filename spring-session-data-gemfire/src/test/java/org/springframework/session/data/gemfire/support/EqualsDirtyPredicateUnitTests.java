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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Unit Tests for {@link EqualsDirtyPredicate}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.session.data.gemfire.support.EqualsDirtyPredicate
 * @since 2.1.2
 */
public class EqualsDirtyPredicateUnitTests {

	@Test
	public void isDirtyIsIsNullSafe() {

		assertThat(EqualsDirtyPredicate.INSTANCE.isDirty(null, null)).isFalse();
		assertThat(EqualsDirtyPredicate.INSTANCE.isDirty("one", null)).isTrue();
		assertThat(EqualsDirtyPredicate.INSTANCE.isDirty(null, "one")).isTrue();
	}

	@Test
	public void isDirtyWithSameObjectReturnsFalse() {

		Entry keyValue = Entry.newEntry(1, "one");

		assertThat(EqualsDirtyPredicate.INSTANCE.isDirty(keyValue, keyValue));
	}

	@Test
	public void isDirtyWithEqualObjectsReturnsFalse() {

		Entry entryOne = Entry.newEntry(1, "one");
		Entry entryTwo = Entry.newEntry(1, "one");

		assertThat(EqualsDirtyPredicate.INSTANCE.isDirty(entryOne, entryTwo));
	}

	@Test
	public void isDirtyWithDifferentObjectsReturnsFalse() {

		Entry entryOne = Entry.newEntry(1, "one");
		Entry entryTwo = Entry.newEntry(2, "two");

		assertThat(EqualsDirtyPredicate.INSTANCE.isDirty(entryOne, entryTwo));
	}

	@Data
	@EqualsAndHashCode
	@RequiredArgsConstructor(staticName = "newEntry")
	static class Entry {

		@NonNull
		private final Object key;

		@NonNull
		private final Object value;

		@Override
		public String toString() {
			return String.format("%1$s = %2$s", getKey(), getValue());
		}
	}
}
