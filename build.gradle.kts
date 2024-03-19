/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  id("java")
  id("idea")
  id("eclipse")
  id("maven-publish")
  alias(libs.plugins.ben.manes.versions)
  alias(libs.plugins.littlerobots.version.catalog.update)
}

group="com.vmware.gemfire"

allprojects {
  configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "minutes")
  }
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
    // keep all libraries that aren't used in the project
    keepUnusedLibraries = true
    // keep all plugins that aren't used in the project
    keepUnusedPlugins = true
  }

}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    !isPatch(candidate.version, currentVersion)
  }
}

fun isPatch(candidateVersion: String, currentVersion: String): Boolean {
  val candidateSplit = candidateVersion.split(".")
  val currentSplit = currentVersion.split(".")

  val strings = listOf("rc", "alpha", "beta")

  if (strings.filter { candidateVersion.uppercase().contains(it) }.toList().isNotEmpty()) {
    return false
  }

  if (currentSplit.size == 3) {
    if (candidateSplit.size == currentSplit.size) {
      return if (candidateSplit[0] != currentSplit[0]) {
        false
      } else if (candidateSplit[1] != currentSplit[1]) {
        false
      } else {
        true
      }
    }
  } else {
    return false
  }
  return false
}
