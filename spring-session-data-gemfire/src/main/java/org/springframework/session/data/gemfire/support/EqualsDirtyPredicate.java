/*
 * Copyright 2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.support;

import org.springframework.lang.Nullable;

/**
 * {@link EqualsDirtyPredicate} is an {@link IsDirtyPredicate} strategy interface implementation that determines
 * whether the {@link Object new value} is dirty by comparing the {@link Object new value}
 * with the {@link Object old value} using the {@link Object#equals(Object)} method.
 *
 * @author John Blum
 * @see IsDirtyPredicate
 * @since 2.1.2
 */
@SuppressWarnings("unused")
public class EqualsDirtyPredicate implements IsDirtyPredicate {

	public static final EqualsDirtyPredicate INSTANCE = new EqualsDirtyPredicate();

	/**
	 * Determines whether the {@link Object new value} is dirty by comparing the {@link Object new value}
	 * with the {@link Object old value} using the {@link Object#equals(Object)} method.
	 *
	 * This method is {@literal null-safe}.
	 *
	 * @param oldValue {@link Object} referring to the previous value.
	 * @param newValue {@link Object} referring to the new value.
	 * @return a boolean value indicating whether the {@link Object new value} is dirty by comparing
	 * the {@link Object new value} with the {@link Object old value} using the {@link Object#equals(Object)} method.
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean isDirty(@Nullable Object oldValue, @Nullable Object newValue) {

		return newValue != null
			? !newValue.equals(oldValue)
			: oldValue != null;
	}
}
