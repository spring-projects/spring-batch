# Spring Batch [![build status](https://build.spring.io/plugins/servlet/wittified/build-status/BATCH-GRAD)](https://build.spring.io/browse/BATCH-GRAD)

Spring Batch is a lightweight, comprehensive batch framework designed to enable the development of robust batch applications vital for the daily operations of enterprise systems.  Spring Batch builds upon the productivity, POJO-based development approach, and general ease of use capabilities people have come to know from the [Spring Framework](https://github.com/spring-projects/spring-framework), while making it easy for developers to access and leverage more advanced enterprise services when necessary.

If you are looking for a runtime orchestration tool for your Batch applications, or need a management console to view current and historic executions, take a look at [Spring Cloud Data Flow](https://cloud.spring.io/spring-cloud-dataflow/).  It is an orchestration tool for deploying and executing data integration based microservices including Spring Batch applications.

## Code of Conduct

Please see our [code of conduct](https://github.com/spring-projects/.github/blob/master/CODE_OF_CONDUCT.md).

## Reporting Security Vulnerabilities

Please see our [Security policy](https://github.com/spring-projects/spring-batch/security/policy).

# Building from Source

Clone the git repository using the URL on the Github home page:

    $ git clone git@github.com:spring-projects/spring-batch.git
    $ cd spring-batch

## Command Line

Gradle is the build tool used for Spring Batch. You can build the project via the command:

    $ ./gradlew build

If you want to perform a full build with all integration tests, ensure you have Docker installed then run:

    $ ./gradlew build -Palltests

## Spring Tool Suite (STS)
In STS (or any Eclipse distro or other IDE with Gradle support), import the module directories as existing projects.  They should compile and the tests should run with no additional steps.

# Getting Started Using Spring Boot
This is the quickest way to get started with a new Spring Batch project.  You find the Getting Started Guide for Spring
Batch on Spring.io: [Creating a Batch Service](https://spring.io/guides/gs/batch-processing/)

# Getting Started Using Spring Tool Suite (STS)

It requires an internet connection for download, and access to a Maven repository (remote or local).

* Download STS version 3.4.* (or better) from the [Spring website](https://spring.io/tools/sts/).  STS is a free Eclipse bundle with many features useful for Spring developers.
* Go to `File->New->Spring Template Project` from the menu bar (in the Spring perspective).
* The wizard has a drop down with a list of template projects.  One of them is a "Simple Spring Batch Project".  Select it and follow the wizard.
* A project is created with all dependencies and a simple input/output job configuration.  It can be run using a unit test, or on the command line (see instructions in the pom.xml).

# Getting Help

Read the main project [website](https://projects.spring.io/spring-batch/) and the [User Guide](https://docs.spring.io/spring-batch/docs/current/reference/). 
Look at the source code and the Javadocs.
For more detailed questions, use [StackOverflow](https://stackoverflow.com/questions/tagged/spring-batch).
If you are new to Spring as well as to Spring Batch, look for information about [Spring projects](https://spring.io/projects).

# Contributing to Spring Batch

Here are some ways for you to get involved in the community:

* Get involved with the Spring Batch community on [Twitter](https://twitter.com/springbatch) and [StackOverflow](https://stackoverflow.com/questions/tagged/spring-batch) by responding to questions and joining the debate.
* Create [issues](https://github.com/spring-projects/spring-batch/issues) for bugs and new features and comment and vote on the ones that you are interested in.
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](https://help.github.com/forking/).  If you want to contribute code this way, please familiarize yourself with the process outlined for contributing to Spring projects here: [Contributor Guidelines](https://github.com/spring-projects/spring-batch/blob/master/CONTRIBUTING.md).
* Watch for upcoming articles on Spring by [subscribing](feed://assets.spring.io/drupal/node/feed.xml) to [spring.io](https://spring.io).

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup).  Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join the core team, and given the ability to merge pull requests.
