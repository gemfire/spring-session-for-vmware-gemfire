/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.FileInputStream
import java.util.*

pluginManagement {
    includeBuild("build-tools/gemfire-server-integration-test-plugin")
    includeBuild("build-tools/publishing")
    includeBuild("build-tools/convention-plugins")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val properties = Properties()
            properties.load(FileInputStream("gradle.properties"))
            versionOverrideFromProperties(this, properties)
        }
    }
}

private fun versionOverrideFromProperty(versionCatalogBuilder: VersionCatalogBuilder, propertyName: String, propertiesFile: Properties): String {
    val propertyValue = providers.systemProperty(propertyName).getOrElse(propertiesFile.getProperty(propertyName))

    return versionCatalogBuilder.version(propertyName, propertyValue)
}

private fun versionOverrideFromProperties(versionCatalogBuilder: VersionCatalogBuilder, properties: Properties) {
    versionOverrideFromProperty(versionCatalogBuilder, "gemfireVersion", properties)
    versionOverrideFromProperty(versionCatalogBuilder, "springDataGemFireVersion", properties)
    versionOverrideFromProperty(versionCatalogBuilder, "springTestGemFireVersion", properties)
}

rootProject.name = "spring-session-data-gemfire"
include(":spring-session-data-gemfire")
project(":spring-session-data-gemfire").name = "spring-session-data-gemfire"


