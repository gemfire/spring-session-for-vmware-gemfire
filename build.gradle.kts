/*
 * Copyright 2023-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import nl.littlerobots.vcu.plugin.versionSelector

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
  toolchain { languageVersion = JavaLanguageVersion.of(8) }
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
    isAllowedUpdate(it.candidate.version, it.currentVersion, allowMajor, allowMinor)
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
