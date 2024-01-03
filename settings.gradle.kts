/*
 * Copyright (c) VMware, Inc. 2023-2024. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.FileInputStream
import java.util.*

pluginManagement {
    includeBuild("build-tools/gemfire-server-integration-test-plugin")
    includeBuild("build-tools/publishing")
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


