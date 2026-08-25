/*
 * Copyright 2023-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import nebula.plugin.responsible.TestFacetDefinition
import java.io.FileInputStream
import java.util.*

buildscript {
  dependencies {
    classpath("com.google.cloud:google-cloud-storage:2.30.2")
  }
}

plugins {
  id("java-library")
  id("maven-publish")
  alias(libs.plugins.lombok)
  alias(libs.plugins.nebula.facet)
  alias(libs.plugins.nebula.facet.integration)
  id("gemfire-repo-artifact-publishing")
  id("gemfire-artifactory")
}

description = "Spring Session For VMware GemFire"


java {
  withJavadocJar()
  withSourcesJar()
  toolchain { languageVersion = JavaLanguageVersion.of(8) }
}

val testJarClasspath = configurations.create("testJarClasspath")

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-parameters")
}

val baseGemFireVersion: String by project
val baseSpringVersion: String by project

tasks.named<Javadoc>("javadoc") {
  title =
    "Spring Session $baseSpringVersion for VMware GemFire $baseGemFireVersion Java API Reference"
  isFailOnError = false
}

publishingDetails {
  artifactName.set("spring-session-$baseSpringVersion-gemfire-$baseGemFireVersion")
  longName.set("Spring Session VMware GemFire")
  description.set("Spring Session $baseSpringVersion For VMware GemFire")
}

facets {
  named("integTest") {
    parentSourceSet = "main"
    (this as TestFacetDefinition).testTaskName = "integrationTest"
    this.includeInCheckLifecycle = false
  }
}

dependencies {
  implementation(platform(libs.spring.framework.bom))
  implementation(platform(libs.spring.security.bom))

  implementation("org.springframework:spring-context-support")
//  implementation("org.springframework:spring-jcl")

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

    testImplementation(libs.awaitility)
    testCompileOnly(libs.jakarta.servlet.api)
    testImplementation(libs.multithreadedtc)
    testImplementation(libs.spring.data.gemfire.test.framework)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.logback.classic)
    testImplementation(libs.log4j.over.slf4j)
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-web")

  "integTestImplementation"(libs.bundles.gemfire.dependencies)
  "integTestImplementation"(libs.junit)
  "integTestImplementation"(libs.assertj.core)
  "integTestImplementation"(libs.mockito.core)
  "integTestImplementation"(libs.multithreadedtc)
  "integTestImplementation"(libs.logback.classic)
  "integTestImplementation"(libs.log4j.over.slf4j)
  "integTestImplementation"(libs.findbugs.jsr305)
  "integTestImplementation"(libs.spring.shell)
  "integTestImplementation"("org.springframework:spring-test")
  "integTestImplementation"(libs.spring.data.gemfire.test.framework)
  "integTestImplementation"(libs.gemfire.testcontainers)

  testJarClasspath(libs.spring.session.core)
  testJarClasspath(libs.spring.security.bom)
  testJarClasspath("org.springframework.security:spring-security-core")
  testJarClasspath(libs.spring.data.gemfire) {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
  }
}

tasks {
  register("copyJavadocsToBucket") {
    dependsOn(named("javadocJar"))
    doLast {
      val storage =
        StorageOptions.newBuilder().setProjectId(project.properties["docsGCSProject"].toString()).setCredentials(
          GoogleCredentials.fromStream(FileInputStream(project.properties["docsGCSProjectCredentials"].toString()))).build().getService()
      val blobId = BlobId.of(
        project.properties["docsGCSBucket"].toString(),
        "${publishingDetails.artifactName.get()}/${project.version}/${named("javadocJar").get().outputs.files.singleFile.name}"
      )
      val blobInfo = BlobInfo.newBuilder(blobId).build()
      storage.createFrom(blobInfo, named("javadocJar").get().outputs.files.singleFile.toPath())
    }
  }
  named<ProcessResources>("processIntegTestResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
}

tasks.register<Jar>("testJar") {
  from(sourceSets.getByName("integTest").output)
  from(sourceSets.main.get().output)
  from(testJarClasspath.map { zipTree(it) })
  archiveFileName = "testJar.jar"
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Test>("integrationTest",Test::class.java) {
  dependsOn("testJar")
  forkEvery = 1
  maxParallelForks = 1
  this.outputs.upToDateWhen { _ -> false }

  val springTestGemfireDockerImage: String by project
  System.err.println("Spring Docker image: $springTestGemfireDockerImage")
  systemProperty("spring.test.gemfire.docker.image", springTestGemfireDockerImage)
  systemProperty("TEST_JAR_PATH", tasks.getByName<Jar>("testJar").outputs.files.singleFile.absolutePath)
  filter { includeTestsMatching("*IntegrationTests") }

}

tasks.named<Test>("test") {
  forkEvery = 1
  maxParallelForks = 1
  this.outputs.upToDateWhen { _ -> false }
  filter { excludeTestsMatching("*IntegrationTests") }
}

tasks.named("build"){
  dependsOn("integrationTest")
  dependsOn("test")
}
