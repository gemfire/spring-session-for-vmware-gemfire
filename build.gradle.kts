/*
 * Copyright 2023-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import nl.littlerobots.vcu.plugin.versionSelector
import java.net.HttpURLConnection
import java.net.URI

plugins {
  id("java")
  id("idea")
  id("eclipse")
  id("maven-publish")
  alias(libs.plugins.littlerobots.version.catalog.update)
  id("gemfire-artifactory")
}

// Suppress warning from gemfire-artifactory plugin. We need the module to be on this project in order to get buildInfo
// uploaded, but there is no artifact on the root project, so we skip that part.
tasks.artifactoryPublish {
  skip = true
}

group="com.vmware.gemfire"

allprojects {
  configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "minutes")
  }
}

java {
  withJavadocJar()
  withSourcesJar()
  toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

versionCatalogUpdate {
  // These options will be set as default for all version catalogs
  sortByKey = true
  // Referenced that are pinned are not automatically updated.
  // They are also not automatically kept however (use keep for that).
  pin {
  }
  keep {
    keepUnusedVersions = true
  }

  // vCU v1.x resolves catalog entries directly via its own detached configurations,
  // independently of DependencyUpdatesTask. Without this selector the rejectVersionIf
  // filter above is bypassed for that second resolution path (e.g. GemFire compileOnly
  // deps that only appear in subprojects). Mirror the same logic here so both paths
  // apply isAllowedUpdate consistently.
  versionSelector {
    val allowMajor = project.hasProperty("updateMajor")
    val allowMinor = project.hasProperty("updateMinor")
    val candidate = it.candidate
    if (!isAllowedUpdate(candidate.version, it.currentVersion, allowMajor, allowMinor)) {
      false
    } else if (candidate.group == "com.vmware.gemfire") {
      // com.vmware.gemfire artifacts are expected to come from our commercial repos;
      // no public-availability check applies to them.
      true
    } else {
      // Our internal repos mirror/aggregate several other Broadcom-internal repos
      // (e.g. commercially patched Spring builds) alongside com.vmware.gemfire
      // artifacts, so version listings for non-gemfire groups can include
      // commercial-only versions (e.g. org.springframework:spring-tx:5.3.50) that
      // don't exist publicly. Since every non-gemfire library we track here is meant
      // to stay on publicly available versions, reject any candidate that doesn't
      // actually resolve from Maven Central.
      isPubliclyAvailable(candidate.group, candidate.module, candidate.version)
    }
  }

}

fun isPubliclyAvailable(group: String, module: String, version: String): Boolean {
  val path = group.replace(".", "/")
  val url = "https://repo.maven.apache.org/maven2/$path/$module/$version/$module-$version.pom"
  return try {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.requestMethod = "HEAD"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    val code = connection.responseCode
    connection.disconnect()
    code == 200
  } catch (e: java.io.IOException) {
    false
  }
}

fun isAllowedUpdate(
  candidateVersion: String,
  currentVersion: String,
  allowMajor: Boolean,
  allowMinor: Boolean
): Boolean {
  // Exclude non-stable / pre-release candidates.
  val nonStableMarkers = listOf("alpha", "beta", "rc", "snapshot", "dev", "preview", "build", "milestone")
  if (nonStableMarkers.any { candidateVersion.contains(it, ignoreCase = true) }) {
    return false
  }
  // Also catch milestone shorthand like 4.0.0.M1 or 6.0.0-M2.
  if (candidateVersion.contains(Regex("""[.\-][Mm]\d"""))) {
    return false
  }

  // Normalize Gradle version ranges (e.g., "[10.2,10.3)" -> "10.2").
  val cleanCurrentVersion = if (currentVersion.startsWith("[") || currentVersion.startsWith("(")) {
    currentVersion
      .replace("[", "")
      .replace("]", "")
      .replace("(", "")
      .replace(")", "")
      .split(",")
      .first()
      .trim()
  } else {
    currentVersion
  }

  if (allowMajor) return true

  // Extract major and minor from a dot-separated version string.
  // Parsing stops at the first non-numeric segment (e.g. ".RELEASE" is ignored).
  // Returns null if major or minor cannot be determined.
  fun parseMajorMinor(v: String): Pair<Int, Int>? {
    val parts = v.split(".")
    val major = parts.getOrNull(0)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: return null
    val minor = parts.getOrNull(1)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: return null
    return major to minor
  }

  val (currentMajor, currentMinor) = parseMajorMinor(cleanCurrentVersion) ?: return false
  val (candidateMajor, candidateMinor) = parseMajorMinor(candidateVersion) ?: return false

  // Major must always match.
  if (currentMajor != candidateMajor) return false

  if (allowMinor) return true

  // The lock boundary in patch mode is major.minor.
  // Anything to the right — 3rd component, 4th component, or changes in component count
  // (e.g. 10.17.0 -> 10.17.1.0, 4.0.6 -> 4.0.6.1, 4.0.6.1 -> 4.0.7) — is a patch/hotfix
  // update and is allowed as long as major and minor are unchanged.
  return currentMinor == candidateMinor
}
