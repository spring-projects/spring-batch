<img align="right" src="spring-batch-docs/src/main/asciidoc/images/spring-batch.png" width="200" height="200">

# Spring Batch [![build status](https://github.com/spring-projects/spring-batch/actions/workflows/continuous-integration.yml/badge.svg)](https://github.com/spring-projects/spring-batch/actions/workflows/continuous-integration.yml)

Spring Batch is a lightweight, comprehensive batch framework designed to enable the development of robust batch applications vital for the daily operations of enterprise systems.  Spring Batch builds upon the productivity, POJO-based development approach, and general ease of use capabilities people have come to know from the [Spring Framework](https://github.com/spring-projects/spring-framework), while making it easy for developers to access and leverage more advanced enterprise services when necessary.

If you are looking for a runtime orchestration tool for your Batch applications, or need a management console to view current and historic executions, take a look at [Spring Cloud Data Flow](https://cloud.spring.io/spring-cloud-dataflow/).  It is an orchestration tool for deploying and executing data integration based microservices including Spring Batch applications.

# Getting Started

## Guide

This is the quickest way to get started with a new Spring Batch project based on Spring Boot. You find the Getting Started Guide here: [Creating a Batch Service](https://spring.io/guides/gs/batch-processing/).

## Samples

You can find several samples to try out here: [Spring Batch Samples](https://github.com/spring-projects/spring-batch/tree/main/spring-batch-samples).

# Getting Help

If you have a question or a support request, please open a new discussion on [GitHub Discussions](https://github.com/spring-projects/spring-batch/discussions)
or ask a question on [StackOverflow](https://stackoverflow.com/questions/tagged/spring-batch).

Please do **not** create issues on the [Issue Tracker](https://github.com/spring-projects/spring-batch/issues) for questions or support requests.
We would like to keep the issue tracker **exclusively** for bug reports and feature requests.

# Reporting issues

Spring Batch uses [GitHub Issues](https://github.com/spring-projects/spring-batch/issues) to record bugs and feature requests. If you want to raise an issue, please follow the recommendations below:

* Before you open an issue, please search the issue tracker to see if someone has already reported the problem. If the issue doesn't already exist, create a new issue.
* Please provide as much information as possible in the issue report by following the [Issue Reporting Template](https://github.com/spring-projects/spring-batch/blob/main/.github/ISSUE_TEMPLATE/bug_report.md).
* If you need to paste code or include a stack trace, please use Markdown escapes (```) before and after your text.

For non trivial bugs, please create a test case or a project that replicates the problem and attach it to the issue, as detailed in the [Issue Reporting Guidelines](https://github.com/spring-projects/spring-batch/blob/main/ISSUE_REPORTING.md).

# Reporting Security Vulnerabilities

Please see our [Security policy](https://github.com/spring-projects/spring-batch/security/policy).

# Building from Source

## Using the Command Line

Clone the git repository using the URL on the Github home page:

    $ git clone git@github.com:spring-projects/spring-batch.git
    $ cd spring-batch

Maven is the build tool used for Spring Batch. You can build the project with the following command:

    $ ./mvnw package

If you want to perform a full build with all integration tests, then run:

    $ ./mvnw verify

Please note that some integration tests are based on Docker, so please make sure to have Docker up and running before running a full build.

To generate the reference documentation, run the following commands:

```
$ ./mvnw javadoc:aggregate
$ cd spring-batch-docs
$ ../mvnw site
```

The reference documentation can be found in `spring-batch-docs/target`.

## Using Docker

If you want to build the project in a Docker container, you can proceed as follows:

```
$> docker run -it --mount type=bind,source="$(pwd)",target=/spring-batch maven:3-openjdk-17 bash
#> cd spring-batch
#> ./mvnw package
```

This will mount the source code that you cloned previously on the host inside the container.
If you want to work on a copy of the source code inside the container (no side effects on the host),
you can proceed as follows:

```
$> docker run -it maven:3-openjdk-17 bash
#> git clone https://github.com/spring-projects/spring-batch.git
#> cd spring-batch
#> ./mvnw package
```

# Contributing to Spring Batch

We welcome contributions in any kind! Here are some ways for you to contribute to the project:

* Get involved with the Spring Batch community on [Twitter](https://twitter.com/springbatch), [GitHub Discussions](https://github.com/spring-projects/spring-batch/discussions) and [StackOverflow](https://stackoverflow.com/questions/tagged/spring-batch) by responding to questions and joining the debate.
* Create [issues](https://github.com/spring-projects/spring-batch/issues) for bugs and new features or comment and vote on the ones that you are interested in.
* Help us reproduce issues marked with [status: need-help-to-reproduce](https://github.com/spring-projects/spring-batch/labels/status%3A%20need-help-to-reproduce) by following the [Issue Reporting Guidelines](https://github.com/spring-projects/spring-batch/blob/main/ISSUE_REPORTING.md).
* Github is for social coding: if you want to write code, we encourage contributions through pull requests. If you want to contribute code this way, please familiarize yourself with the process outlined here: [Contributor Guidelines](https://github.com/spring-projects/spring-batch/blob/main/CONTRIBUTING.md).
* Watch for Spring Batch related articles on [spring.io](https://spring.io).

Before we accept pull requests, we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup).  Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join the core team, and given the ability to merge pull requests.

# Code of Conduct

Please see our [code of conduct](https://github.com/spring-projects/.github/blob/main/CODE_OF_CONDUCT.md).

# License

Spring Batch is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).