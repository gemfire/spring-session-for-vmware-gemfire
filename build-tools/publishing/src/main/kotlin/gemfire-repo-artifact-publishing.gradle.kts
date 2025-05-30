/*
 * Copyright 2023-2025 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.vmware.gemfire.publishing.extension.ManifestExtension
import org.gradle.jvm.tasks.Jar
import java.net.URI

plugins {
    id("maven-publish")

}

// The published bom will constrain versions within gemfire of any subproject with this property set.
project.ext.set("constrainVersionInBom", true)

val license = """
  Copyright (c) VMware, Inc. 2023-2025. All rights reserved.
  SPDX-License-Identifier: Apache-2.0
  """

val publishingDetails = project.extensions.create<ManifestExtension>("publishingDetails")

publishing {
    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                from(components["java"])
                groupId = "com.vmware.gemfire"
                artifactId = publishingDetails.artifactName.get()
                pom {
                    name = publishingDetails.longName.get()
                    description = publishingDetails.description.get()
                    url = "https://tanzu.vmware.com/gemfire"

                    withXml {
                        val providerAsElement = asElement()
                        providerAsElement.insertBefore(
                            providerAsElement.ownerDocument.createComment(license),
                            providerAsElement.firstChild
                        )
                    }
                    scm {
                        connection = "scm:git:https://github.com/gemfire/spring-session-for-vmware-gemfire.git"
                        developerConnection =
                            "scm:git:https://github.com/gemfire/spring-session-for-vmware-gemfire.git"
                        url = "https://github.com/gemfire/spring-session-for-vmware-gemfire"
                    }
                }
            }
            repositories {
            }
        }
    }
}


tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

tasks.withType(GenerateModuleMetadata::class.java) {
    enabled = false
}

gradle.taskGraph.whenReady {
    tasks.withType(Jar::class.java).forEach { jar ->
        jar.doFirst {
            val attributes = jar.manifest.attributes
            attributes["Manifest-Version"] = "1.0"
            attributes["Created-By"] = System.getProperty("user.name")
            attributes["Title"] = publishingDetails.longName
            attributes["Version"] = version
            attributes["Organization"] = "VMware, Inc."
        }
        jar.metaInf {
            from("$rootDir/LICENSE.txt")
            from("$rootDir/NOTICE")
        }
    }
}
