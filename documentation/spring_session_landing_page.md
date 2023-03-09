---
title: Spring Session for VMware GemFire Quick Start
---

Spring Session for VMware GemFire provides an implementation of the core Spring Session framework using VMware GemFire to manage a user’s Session information by leveraging Spring Data for VMware GemFire.

By integrating with VMware GemFire, you have the full power of this technology (Strong Consistency, Low Latency, High Availability, Resiliency, etc.) at your fingertips in your Spring Boot or Spring Data applications.

This reference guide explains how to add the Spring Session for VMware GemFire dependency to your Spring Boot or Spring Data project. Once the dependency has been added, refer to the [Spring Boot for Apache Geode Reference Guide](https://docs.spring.io/spring-boot-data-geode-build/current/reference/html5/) and [Spring Session](https://docs.spring.io/spring-session-data-geode/docs/current/reference/html5/) for in-depth information about using the dependency.


## Add Spring Data for VMware GemFire to a Project

The Spring Session for VMware GemFire dependencies are available from the [Pivotal Commercial Maven Repository](https://commercial-repo.pivotal.io/login/auth). Access to the Pivotal Commercial Maven Repository requires a one-time registration step to create an account.

Spring Session for VMware GemFire requires users to add the GemFire repository to their projects.

To add Spring Session for VMware GemFire to a project:

1. In a browser, navigate to the [Pivotal Commercial Maven Repository](https://commercial-repo.pivotal.io/login/auth).

1. Click the **Create Account** link.

1. Complete the information in the registration page.

1. Click **Register**.

1. After registering, you will receive a confirmation email. Follow the instruction in this email to activate your account.

1. After account activation, log in to the [Pivotal Commercial Maven Repository](https://commercial-repo.pivotal.io/login/auth) to access the configuration information found in [gemfire-release-repo](https://commercial-repo.pivotal.io/repository/gemfire-release-repo).

1. Add the GemFire repository to your project:

    * **Maven**: Add the following block to the `pom.xml` file:

        ```
        <repository>
            <id>gemfire-release-repo</id>
            <name>Pivotal GemFire Release Repository</name>
            <url>https://commercial-repo.pivotal.io/data3/gemfire-release-repo/gemfire</url>
        </repository>
        ```

    * **Gradle**: Add the following block to the `repositories` section of the `build.gradle` file:

        ```
        repositories {
            mavenCentral()
            maven {
                credentials {
                    username "$gemfireRepoUsername"
                    password "$gemfireRepoPassword"
                }
                url = uri("https://commercial-repo.pivotal.io/data3/gemfire-release-repo/gemfire")
            }
        }
        ```

1. Add your Pivotal Commercial Maven Repository credentials.

    * **Maven**: Add the following to the `.m2/settings.xml` file. Replace `MY-USERNAME@example` and `MY-DECRYPTED-PASSWORD` with your Pivotal Commercial Maven Repository credentials.

        ```
        <settings>
            <servers>
                <server>
                    <id>gemfire-release-repo</id>
                    <username>MY-USERNAME@example.com</username>
                    <password>MY-DECRYPTED-PASSWORD</password>
                </server>
            </servers>
        </settings>
        ```

    * **Gradle**: Add the following to the local (`.gradle/gradle.properties`) or project `gradle.properties` file. Replace `MY-USERNAME@example` and `MY-DECRYPTED-PASSWORD` with your Pivotal Commercial Maven Repository credentials.

        ```
        gemfireRepoUsername=MY-USERNAME@example.com 
        gemfireRepoPassword=MY-DECRYPTED-PASSWORD
        ```

1. After you have set up the repository and credentials, add the Spring Session for VMware GemFire dependency to your application.

    The `<artifactId>` should match the `major.minor` version of Spring Session and GemFire that your application is connecting with. For example, if you are using Spring Session 2.7.x and GemFire version 9.15.3, then the `<artifactId>` will be `spring-session-2.7-gemfire-9.15`.

    For version 1.0.0:

    * **Maven**:

        ```
        <dependency>
            <groupId>com.vmware.gemfire</groupId>
            <artifactId>spring-session-2.7-gemfire-9.15</artifactId>
            <version>1.0.0</version>
        </dependency>

        ```

    * **Gradle**:

        ```
        implementation "com.vmware.gemfire:spring-session-2.7-gemfire-9.15:1.0.0"
        ```

    For version 1.1.0 and later:

    Starting in version 1.1.0, you will be required to "Bring Your Own GemFire," which will allow for improved flexibility with GemFire patch versions. In addition to the Spring Session for VMware GemFire dependency, you must add an explicit dependency on the desired version of GemFire. The required dependencies will differ for clients and servers.

    For clients:

    * **Maven**:

        ```
        <dependency>
            <groupId>com.vmware.gemfire</groupId>
            <artifactId>spring-session-2.7-gemfire-9.15</artifactId>
            <version>1.1.0</version>
        </dependency>
        <dependency>
            <groupId>com.vmware.gemfire</groupId>
            <artifactId>geode-core</artifactId>
            <version>9.15.4</version>
        </dependency>
        <!--if using continuous queries-->
        <dependency>
            <groupId>com.vmware.gemfire</groupId>
            <artifactId>geode-cq</artifactId>
            <version>9.15.4</version>
        </dependency>
        ```

    * **Gradle**:

        ```
        implementation "com.vmware.gemfire:spring-session-2.7-gemfire-9.15:1.1.0"
        implementation "com.vmware.gemfire:geode-core:9.15.4"
        // if using continuous queries
        implementation "com.vmware.gemfire:geode-cq:9.15.4"
        ```

    For servers:

    NOTE: The server dependencies are only required if the user is starting an embedded GemFire server using Spring.

    * **Maven**:

        ```
        <dependency>
            <groupId>com.vmware.gemfire</groupId>
            <artifactId>spring-session-2.7-gemfire-9.15</artifactId>
            <version>1.1.0</version>
        </dependency>
        <dependency>
            <groupId>com.vmware.gemfire</groupId>
            <artifactId>geode-sever-all</artifactId>
            <version>9.15.4</version>
            <exclusions>
                <exclusion>
                    <groupId>com.vmware.gemfire</groupId>
                    <artifactId>geode-log4j</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        ```

    * **Gradle**:

        ```
        implementation "com.vmware.gemfire:spring-session-2.7-gemfire-9.15:1.1.0"
        implementation ("com.vmware.gemfire:geode-server-all:9.15.4"){exclude group: 'com.vmware.gemfire', module: 'geode-log4j'}
        ```

1. Your application is now ready to connect with your GemFire instance.

## Reference Guide

For further information, refer to the [Spring Boot for Apache Geode Reference Guide](https://docs.spring.io/spring-boot-data-geode-build/current/reference/html5/) and [Spring Session](https://docs.spring.io/spring-session-data-geode/docs/current/reference/html5/) reference documentation.
