/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.session.data.gemfire.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link GemFireUtils} is an abstract, extensible utility class for working with Apache Geode and Pivotal GemFire
 * objects and types.
 *
 * @author John Blum
 * @see Cache
 * @see ClientCache
 * @see Region
 * @see org.apache.geode.cache.client.ClientCache
 * @since 1.1.0
 */
public abstract class GemFireUtils {

	/**
	 * Null-safe method to close the given {@link Closeable} object.
	 *
	 * @param obj the {@link Closeable} object to close.
	 * @return true if the {@link Closeable} object is not null and was successfully
	 * closed, otherwise return false.
	 * @see Closeable
	 */
	public static boolean close(Closeable obj) {

		if (obj != null) {
			try {
				obj.close();
				return true;
			}
			catch (IOException ignore) { }
		}

		return false;
	}

	/**
	 * Determines whether the given {@link ClientRegionShortcut} is local only.
	 *
	 * @param shortcut {@link ClientRegionShortcut} to evaluate.
	 * @return a boolean value indicating whether the {@link ClientRegionShortcut} is local or not.
	 * @see ClientRegionShortcut
	 */
	public static boolean isLocal(@Nullable ClientRegionShortcut shortcut) {
		return shortcut != null && shortcut.name().toLowerCase().contains("local");
	}

	/**
	 * Determines whether the given {@link Region} is a non-local, client {@link Region}, a {@link Region}
	 * for which a corresponding server {@link Region} exists.
	 *
	 * @param region {@link Region} to evaluate.
	 * @return a boolean value indicating whether the given {@link Region} is a non-local, client {@link Region},
	 * a {@link Region} for which a corresponding server {@link Region} exists.
	 * @see Region
	 * @see #isPoolConfiguredOrHasServerProxy(Region)
	 */
	public static boolean isNonLocalClientRegion(@Nullable Region<?, ?> region) {

		return Optional.ofNullable(region)
			.filter(GemFireUtils::isPoolConfiguredOrHasServerProxy)
			.map(Region::getRegionService)
			.filter(ClientCache.class::isInstance)
			.map(ClientCache.class::cast)
			.isPresent();
	}

	private static boolean isPoolConfiguredOrHasServerProxy(@Nullable Region<?, ?> region) {
		return isPoolConfigured(region) || hasServerProxy(region);
	}

	private static boolean isPoolConfigured(@Nullable Region<?, ?> region) {

		return Optional.ofNullable(region)
			.map(Region::getAttributes)
			.map(RegionAttributes::getPoolName)
			.filter(StringUtils::hasText)
			.isPresent();
	}

	private static boolean hasServerProxy(@Nullable Region<?, ?> region) {

		//return region instanceof AbstractRegion && ((AbstractRegion) region).hasServerProxy();

		return Optional.ofNullable(region)
			.map(Object::getClass)
			.map(regionType -> ReflectionUtils.findMethod(regionType, "hasServerProxy"))
			.map(hasServerProxyMethod -> ReflectionUtils.invokeMethod(hasServerProxyMethod, region))
			.map(Boolean.TRUE::equals)
			.orElse(false);
	}

	/**
	 * Determines whether the given {@link ClientRegionShortcut} is a proxy-based shortcut.
	 *
	 * "Proxy"-based {@link Region Regions} keep no local state.
	 *
	 * @param shortcut {@link ClientRegionShortcut} to evaluate.
	 * @return a boolean value indicating whether the {@link ClientRegionShortcut} refers to a Proxy-based shortcut.
	 * @see ClientRegionShortcut
	 */
	public static boolean isProxy(ClientRegionShortcut shortcut) {
		return ClientRegionShortcut.PROXY.equals(shortcut);
	}

	/**
	 * Determines whether the given {@link Region} is a {@literal PROXY}.
	 *
	 * @param region {@link Region} to evaluate as a {@literal PROXY}; must not be {@literal null}.
	 * @return a boolean value indicating whether the {@link Region} is a {@literal PROXY}.
	 * @see DataPolicy
	 * @see Region
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isProxy(Region<?, ?> region) {

		RegionAttributes regionAttributes = region.getAttributes();

		DataPolicy regionDataPolicy = regionAttributes.getDataPolicy();

		return DataPolicy.EMPTY.equals(regionDataPolicy)
			|| Optional.ofNullable(regionDataPolicy)
				.filter(DataPolicy.PARTITION::equals)
				.map(it -> regionAttributes.getPartitionAttributes())
				.filter(partitionAttributes -> partitionAttributes.getLocalMaxMemory() <= 0)
				.isPresent();
	}

	/**
	 * Determines whether the {@link RegionShortcut} is a Proxy-based shortcut.
	 *
	 * "Proxy"-based {@link Region Regions} keep no local state.
	 *
	 * @param shortcut {@link RegionShortcut} to evaluate.
	 * @return a boolean value indicating whether the {@link RegionShortcut} refers to a Proxy-based shortcut.
	 * @see RegionShortcut
	 */
	public static boolean isProxy(RegionShortcut shortcut) {

		switch (shortcut) {
			case PARTITION_PROXY:
			case PARTITION_PROXY_REDUNDANT:
			case REPLICATE_PROXY:
				return true;
			default:
				return false;
		}
	}
}
