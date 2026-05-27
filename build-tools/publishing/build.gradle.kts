/*
 * Copyright 2022-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("groovy-gradle-plugin")
    `kotlin-dsl`
}

repositories {
    addGemFireRepositories(
        providers,
        addMavenCentral = providers.gradleProperty("useMavenCentral").getOrElse("false").toBoolean()
    )
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    implementation("org.nosphere.apache:creadur-rat-gradle:0.7.1")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.42.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.3")
    implementation("me.champeau.gradle:japicmp-gradle-plugin:0.3.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.11.0")
}

fun RepositoryHandler.addGemFireRepositories(
    providers: ProviderFactory,
    addGradlePluginPortal: Boolean = false,
    addMavenCentral: Boolean = false
) {
    val configFilePath = providers.gradleProperty("spring.gemfire.repositories").getOrElse(
        providers.environmentVariable("HOME").get() + "/.gradle/gradleRepositories.json"
    )
    val jsonString = File(configFilePath).readText(Charsets.UTF_8)
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
    if (addGradlePluginPortal) gradlePluginPortal()
    if (addMavenCentral) mavenCentral()
}
