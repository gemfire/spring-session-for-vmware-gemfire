/*
 * Copyright 2024-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
  id("java-library")
  id("idea")
  id("eclipse")
}

repositories {
  val repositoryConfigFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
    providers.environmentVariable("HOME").get() + "/.gradle/gradleRepositories.json"
  )

  val jsonString = File(repositoryConfigFilePath).readText(Charsets.UTF_8)
  val repositories = groovy.json.JsonSlurper().parseText(jsonString) as Map<*, *>
  (repositories["repositories"] as List<*>).filterNotNull().map { entry -> entry as Map<*, *> }
    .forEach { entry ->
      entry.apply {
        maven {
          url = uri(entry["url"]!! as String)
          if (!entry["username"]?.toString().isNullOrBlank()) {
            credentials {
              username = entry["username"] as String
              password = entry["password"] as String
            }
          }
        }
      }
    }
  if (providers.gradleProperty("useMavenCentral").getOrElse("false").toBoolean()) {
    mavenCentral()
  }
}
