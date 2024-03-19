/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.session.data.gemfire.serialization.pdx.support;

import static java.util.stream.StreamSupport.stream;
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeIterable;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.PdxWriter;

/**
 * The {@link ComposablePdxSerializer} class is a composite of {@link PdxSerializer} objects implementing
 * the Composite Software Design Pattern.
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @see org.apache.geode.pdx.PdxSerializer
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class ComposablePdxSerializer implements PdxSerializer, Iterable<PdxSerializer> {

	private final List<PdxSerializer> pdxSerializers;

	public static PdxSerializer compose(PdxSerializer... pdxSerializers) {
		return compose(Arrays.asList(nullSafeArray(pdxSerializers, PdxSerializer.class)));
	}

	public static PdxSerializer compose(Iterable<PdxSerializer> pdxSerializers) {

		List<PdxSerializer> pdxSerializerList =
			stream(nullSafeIterable(pdxSerializers).spliterator(), false)
				.filter(Objects::nonNull).collect(Collectors.toList());

		return (pdxSerializerList.isEmpty() ? null
			: (pdxSerializerList.size() == 1 ? pdxSerializerList.get(0)
			: new ComposablePdxSerializer(pdxSerializerList)));
	}

	private ComposablePdxSerializer(List<PdxSerializer> pdxSerializers) {

		this.pdxSerializers = Optional.ofNullable(pdxSerializers)
			.map(it -> Collections.unmodifiableList(pdxSerializers))
			.orElseThrow(() -> newIllegalArgumentException("PdxSerializers [%s] are required", pdxSerializers));
	}

	@Override
	public Iterator<PdxSerializer> iterator() {
		return this.pdxSerializers.iterator();
	}

	@Override
	public boolean toData(Object obj, PdxWriter out) {

		for (PdxSerializer pdxSerializer : this) {
			if (pdxSerializer.toData(obj, out)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Object fromData(Class<?> type, PdxReader in) {

		for (PdxSerializer pdxSerializer : this) {

			Object obj = pdxSerializer.fromData(type, in);

			if (obj != null) {
				return obj;
			}
		}

		return null;
	}
}
