package com.vmware.gemfire.gradle.plugins

import org.gradle.api.Project



class DependencySetHandler {
  String group
  String version
  Project project

  DependencySetHandler(String group, String version, Project project) {
    this.group = group
    this.version = version
    this.project = project
  }

  void entry(String name) {
    this.project.dependencies.constraints {
      api(group: group, name: name, version: version)
    }
  }


}
