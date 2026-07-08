/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    includeBuild("build-tools/gemfire-server-integration-test-plugin")
    includeBuild("build-tools/publishing")
    includeBuild("build-tools/convention-plugins")
    repositories {
        val repositoryConfigFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
            "${providers.environmentVariable("HOME").get()}/.gradle/gradleRepositories.json"
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
        if (providers.gradleProperty("useMavenLocal").getOrElse("true").toBoolean()) {
            mavenLocal()
        }
        if (providers.gradleProperty("useMavenCentral").getOrElse("false").toBoolean()) {
            gradlePluginPortal()
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        addGemFireRepositories(
            providers,
            addMavenLocal = providers.gradleProperty("useMavenLocal").getOrElse("true").toBoolean(),
            addMavenCentral = providers.gradleProperty("useMavenCentral").getOrElse("false").toBoolean()
        )
    }
    versionCatalogs {
        create("libs") {
            overrideProperty("gemfireVersion")
            overrideProperty("springDataGemFireVersion")
            overrideProperty("springVersion")
            overrideProperty("springSecurityVersion")
            overrideProperty("springSessionVersion")
        }
    }
}

fun VersionCatalogBuilder.overrideProperty(property: String) {
    val value = System.getProperty(property)
        ?: (settings as? ExtensionAware)?.extensions?.extraProperties?.let {
            if (it.has(property)) it.get(property) as? String else null
        }

    if (value != null) {
        logger.debug("Overriding $property: $value")
        version(property, value)
    }
}

fun org.gradle.api.artifacts.dsl.RepositoryHandler.addGemFireRepositories(
    providers: org.gradle.api.provider.ProviderFactory,
    addMavenLocal: Boolean = false,
    addMavenCentral: Boolean = false
) {
    if (addMavenLocal) mavenLocal()
    val configFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
        "${providers.environmentVariable("HOME").get()}/.gradle/gradleRepositories.json"
    )
    val jsonString = java.io.File(configFilePath).readText(Charsets.UTF_8)
    val repos = groovy.json.JsonSlurper().parseText(jsonString) as Map<*, *>
    (repos["repositories"] as List<*>).filterNotNull().map { it as Map<*, *> }
        .forEach { entry ->
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
    if (addMavenCentral) mavenCentral()
}

rootProject.name = "spring-session-gemfire"
include("spring-session-data-gemfire")
project(":spring-session-data-gemfire").name = "spring-session-data-gemfire"
