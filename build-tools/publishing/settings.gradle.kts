/*
 * Copyright 2022-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
pluginManagement {
    repositories.gradlePluginPortal()
    repositories.google()
    repositories.maven {
        url = uri("https://repo.spring.io/plugins-release")
    }
}

dependencyResolutionManagement{
    versionCatalogs {
        create("publishLibs"){
            from(files("../../gradle/publish.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}
