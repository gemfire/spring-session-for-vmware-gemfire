/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("groovy-gradle-plugin")
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    implementation("org.nosphere.apache:creadur-rat-gradle:0.7.1")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.42.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.3")
    implementation("me.champeau.gradle:japicmp-gradle-plugin:0.3.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.11.0")
}
