/*
 * Copyright 2024-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.file.Path

plugins {
  id("java-library")
  id("idea")
  id("eclipse")
}

repositories {
  addGemFireRepositories(providers)
}

fun getEtcDirectoryFromProjectPath(path: Path): String {
  var originalPath = path
  for (depth in 0..10) {
    if (originalPath.toFile().listFiles { pathname -> pathname.name == "etc" }!!.isEmpty()) {
      originalPath = originalPath.parent
    } else {
      originalPath = originalPath.resolve("etc")
      break
    }
  }
  return originalPath.toAbsolutePath().toString()
}
