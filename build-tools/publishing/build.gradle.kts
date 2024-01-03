/*
 * Copyright (c) VMware, Inc. 2022-2024. All rights reserved.
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
    implementation(publishLibs.ben.manes.versions.plugin)
    implementation(publishLibs.kotlin.gradle.plugin)
}
