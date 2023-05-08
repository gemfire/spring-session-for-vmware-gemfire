---
title: Spring Session for VMware GemFire
---

<!-- 
 Copyright (c) VMware, Inc. 2022. All rights reserved.
 Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 agreements. See the NOTICE file distributed with this work for additional information regarding
 copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance with the License. You may obtain a
 copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software distributed under the License
 is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 or implied. See the License for the specific language governing permissions and limitations under
 the License.
-->

Spring Session for VMware GemFire provides an implementation of the core Spring Session framework using VMware GemFire to manage a user’s Session information by leveraging Spring Data for VMware GemFire. It provides transparent integration with:

- **HttpSession**: Enables the `HttpSession` to be clustered without being tied to an application container-specific solution.
- **REST API**: Allows the session ID to be provided in the protocol header to work with RESTful APIs.
- **WebSocket**: Provides the ability to keep the `HttpSession` alive when receiving WebSocket messages.
- **WebSession**: Allows replacing the Spring WebFlux's `WebSession` in an application container neutral way.

By integrating with VMware GemFire, you have the full power of this technology (Strong Consistency, Low Latency, High Availability, Resiliency, etc.) at your fingertips in your Spring Boot or Spring Data applications.

This initial set of documentation is meant to get users up and running quickly with Spring Session for VMware GemFire.  For more in-depth discussion of features please refer to the  [Spring Boot for Apache Geode Reference Guide](https://docs.spring.io/spring-boot-data-geode-build/current/reference/html5/#geode-session) and [Spring Session for Apache Geode](https://docs.spring.io/spring-session-data-geode/docs/current/reference/html5/) reference documentation.






