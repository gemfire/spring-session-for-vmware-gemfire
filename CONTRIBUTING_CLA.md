# Contributing to spring-session-for-vmware-gemfire

We welcome contributions from the community and first want to thank you for taking the time to contribute!

Please familiarize yourself with the [Code of Conduct](https://github.com/vmware/.github/blob/main/CODE_OF_CONDUCT.md) before contributing.

Before you start working with spring-session-for-vmware-gemfire, please read and sign our Contributor License Agreement [CLA](https://cla.vmware.com/cla/1/preview). If you wish to contribute code and you have not signed our contributor license agreement (CLA), our bot will prompt you to do so when you open a Pull Request. For any questions about the CLA process, please refer to our [FAQ]([https://cla.vmware.com/faq](https://cla.vmware.com/faq)).

## Ways to contribute

We welcome many different types of contributions and not all of them need a Pull request. Contributions may include:

* New features and proposals
* Documentation
* Bug fixes
* Issue Triage
* Answering questions and giving feedback
* Helping to onboard new contributors
* Other related activities

## Getting started

This project provides an integration between [VMware GemFire](https://tanzu.vmware.com/gemfire) and [Spring Session](https://spring.io/projects/spring-session) using [Spring Data For VMware GemFire](https://github.com/gemfire/spring-data-for-vmware-gemfire).

This project has a dependency on [VMware GemFire](https://tanzu.vmware.com/gemfire), which has the requirement that one has access to it, in order to build, run, develop, test it.
Follow the following steps to gain access to the GemFire releases. [Obtaining VMware GemFire from a Maven Repository](https://docs.vmware.com/en/VMware-Tanzu-GemFire/9.15/tgf/GUID-getting_started-installation-obtain_gemfire_maven.html)

This project is dependent on JDK8 and is tested with JDK 1.8.0_352 and uses Gradle.

Once the project is cloned running `./gradlew build` to confirm everything works.


## Contribution Flow

This is a rough outline of what a contributor's workflow looks like:

* Make a fork of the repository within your GitHub account
* Create a topic branch in your fork from where you want to base your work
* Make commits of logical units
* Make sure your commit messages are with the proper format, quality and descriptiveness (see below)
* Push your changes to the topic branch in your fork
* Create a pull request containing that commit

We follow the GitHub workflow and you can find more details on the [GitHub flow documentation](https://docs.github.com/en/get-started/quickstart/github-flow).


### Pull Request Checklist

Before submitting your pull request, we advise you to use the following:

1. Check if your code changes will pass both code linting checks and unit tests.
2. Ensure your commit messages are descriptive. We follow the conventions on [How to Write a Git Commit Message](http://chris.beams.io/posts/git-commit/). Be sure to include any related GitHub issue references in the commit message. See [GFM syntax](https://guides.github.com/features/mastering-markdown/#GitHub-flavored-markdown) for referencing issues and commits.
3. Check the commits and commits messages and ensure they are free from typos.

## Reporting Bugs and Creating Issues

For specifics on what to include in your report, please follow the guidelines in the issue and pull request templates when available.

Bugs and issues are to be reported using GitHub Issues. Typically, issue reports will contain the following:
* Version of project in which the problem has been found
* Description of problem and steps to reproduce.
* Attach logs


## Ask for Help

The best way to reach us with a question when contributing is to ask on:

* Creating GitHub issue
* Responding on original GitHub issue


## Additional Resources

Understanding [Spring Session](https://spring.io/projects/spring-session) is required.
Knowledge of [VMware GemFire](https://tanzu.vmware.com/gemfire) is preferred.

