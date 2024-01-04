/*
 * Copyright (c) VMware, Inc. 2024. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import nebula.plugin.responsible.TestFacetDefinition

/*
 * Copyright (c) VMware, Inc. 2023-2024. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

buildscript {
  dependencies {
    classpath("com.google.cloud:google-cloud-storage:2.30.1")
  }
}

plugins {
  id("java-library")
  id("maven-publish")
  alias(libs.plugins.lombok)
  alias(libs.plugins.nebula.facet)
  id("gemfire-repo-artifact-publishing")
}

description = "Spring Session For VMware GemFire"


java {
  withJavadocJar()
  withSourcesJar()
  toolchain {
    languageVersion = JavaLanguageVersion.of(8)
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}



tasks.named<Javadoc>("javadoc") {
  title =
    "Spring Session 2.7 for VMware GemFire ${getGemFireBaseVersion()} Java API Reference"
  isFailOnError = false
}

publishingDetails {
  artifactName.set("spring-session-2.7-gemfire-${getGemFireBaseVersion()}")
  longName.set("Spring Session VMware GemFire")
  description.set("Spring Session For VMware GemFire")
}

facets {
  create("integrationTest") {
    (this as TestFacetDefinition).includeInCheckLifecycle = false
  }
}

dependencies {
  implementation(platform(libs.spring.framework.bom))
  implementation(platform(libs.spring.security.bom))

  implementation("org.springframework:spring-context-support")
  implementation("org.springframework:spring-jcl")

  api(libs.spring.data.gemfire)
  api(libs.spring.session.core)

  implementation(libs.findbugs.jsr305)
  implementation("org.springframework.security:spring-security-core")
  implementation("org.springframework.security:spring-security-web")
  implementation(libs.spring.tx)
  implementation(libs.javax.annotation.api)

  compileOnly(libs.bundles.gemfire.dependencies)

  compileOnly(libs.jakarta.servlet.api)
  runtimeOnly(libs.jakarta.servlet.api)

  testImplementation(libs.bundles.gemfire.dependencies)

  testCompileOnly(libs.jakarta.servlet.api)
  testImplementation(libs.multithreadedtc)
  testImplementation(libs.spring.test.gemfire)
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit)
  testImplementation(libs.mockito.core)
  testImplementation(libs.logback.classic)
  testImplementation(libs.log4j.over.slf4j)
  testImplementation("org.springframework:spring-test")
  testImplementation("org.springframework:spring-web")

  "integrationTestImplementation"(libs.bundles.gemfire.dependencies)
  "integrationTestImplementation"(libs.junit)
  "integrationTestImplementation"(libs.assertj.core)
  "integrationTestImplementation"(libs.logback.classic)
  "integrationTestImplementation"(libs.log4j.over.slf4j)
  "integrationTestImplementation"(libs.findbugs.jsr305)
  "integrationTestImplementation"(libs.spring.shell)
  "integrationTestImplementation"("org.springframework:spring-test")
  "integrationTestImplementation"(libs.spring.test.gemfire)
}

sourceSets {
  named("integrationTest") {
    java.srcDir(file("src/integrationTest/java"))
    resources.srcDir(file("src/integrationTest/resources"))
  }
}

repositories {
  mavenCentral()
  maven {
    credentials {
      username = property("gemfireRepoUsername") as String
      password = property("gemfireRepoPassword") as String
    }
    url = uri("https://commercial-repo.pivotal.io/data3/gemfire-release-repo/gemfire")
  }
  val additionalMavenRepoURLs = project.findProperty("additionalMavenRepoURLs").toString()
  if (!additionalMavenRepoURLs.isNullOrBlank() && additionalMavenRepoURLs.isNotEmpty()) {
    additionalMavenRepoURLs.split(",").forEach {
      project.repositories.maven {
        this.url = uri(it)
      }
    }
  }
}

tasks {
  register("copyJavadocsToBucket") {
    val javadocJarTask = project.tasks.named("javadocJar")
    dependsOn(javadocJarTask)
    doLast {
      val storage = StorageOptions.newBuilder().setProjectId(property("docsGCSProject").toString())
        .build().getService()
      val javadocJarFiles = javadocJarTask.get().outputs.files
      val blobId = BlobId.of(
        property("docsGCSBucket").toString(),
        "${property("pomProjectArtifactName")}/${project.version}/${javadocJarFiles.singleFile.name}"
      )
      val blobInfo = BlobInfo.newBuilder(blobId).build()
      storage.createFrom(blobInfo, javadocJarFiles.singleFile.toPath())
    }
  }
  named<ProcessResources>("processIntegrationTestResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
}

private fun getSpringSessionBaseVersion(): String {
  return getBaseVersion(property("springSessionVersion").toString())
}

private fun getGemFireBaseVersion(): String {
  return getBaseVersion(property("gemfireVersion").toString())
}

private fun getBaseVersion(version: String): String {
  val split = version.split(".")
  if (split.size < 2) {
    throw RuntimeException("version is malformed")
  }
  return "${split[0]}.${split[1]}"
}

tasks.named<Test>("integrationTest"){
    forkEvery = 1
    maxParallelForks = 1

    filter {
        includeTestsMatching("*.*Tests")
        includeTestsMatching("*.*Test")
    }
}

tasks.named<Test>("test") {
    forkEvery = 1
    maxParallelForks = 1

    filter {
        includeTestsMatching("*.*Tests")
        includeTestsMatching("*.*Test")
    }
}
