# Spring Batch [![build status](https://build.spring.io/plugins/servlet/buildStatusImage/BATCH-TRUNK)](https://build.spring.io/browse/BATCH-TRUNK)

Spring Batch is a lightweight, comprehensive batch framework designed to enable the development of robust batch applications vital for the daily operations of enterprise systems.  Spring Batch builds upon the productivity, POJO-based development approach, and general ease of use capabilities people have come to know from the [Spring Framework](https://github.com/SpringSource/spring-framework), while making it easy for developers to access and leverage more advanced enterprise services when necessary.

If you are looking for a runtime container for your Batch applications, or need a management console to view current and historic executions, take a look at [Spring Batch Admin](http://docs.spring.io/spring-batch-admin).  It is a set of services (Java, JSON, JMX) and an optional web UI that you can use to manage and monitor a Batch system.

# Building from Source

Clone the git repository using the URL on the Github home page:

    $ git clone git://github.com/SpringSource/spring-batch.git
    $ cd spring-batch

## Command Line
Gradle is the build tool used for Spring Batch.  You can perform a full build of Spring Batch via the command:

    $ ./gradlew build

## Spring Tool Suite (STS)
In STS (or any Eclipse distro or other IDE with Maven support), import the module directories as existing projects.  They should compile and the tests should run with no additional steps.

# Getting Started Using Spring Boot
This is the quickest way to get started with a new Spring Batch project.  You find the Getting Started Guide for Spring
Batch on Spring.io: [Creating a Batch Service](http://spring.io/guides/gs/batch-processing/)

# Getting Started Using Spring Tool Suite (STS)

It requires an internet connection for download, and access to a Maven repository (remote or local).

* Download STS version 3.4.* (or better) from the [Spring website](http://spring.io/tools/sts/).  STS is a free Eclipse bundle with many features useful for Spring developers.
* Go to `File->New->Spring Template Project` from the menu bar (in the Spring perspective).
* The wizard has a drop down with a list of template projects.  One of them is a "Simple Spring Batch Project".  Select it and follow the wizard.
* A project is created with all dependencies and a simple input/output job configuration.  It can be run using a unit test, or on the command line (see instructions in the pom.xml).

# Getting Help

Read the main project [website](http://projects.spring.io/spring-batch/) and the [User Guide](http://docs.spring.io/spring-batch/trunk/reference/). Look at the source code and the Javadocs.  For more detailed questions, use the [forum](http://forum.spring.io/forum/spring-projects/batch).  If you are new to Spring as well as to Spring Batch, look for information about [Spring projects](http://spring.io/projects).

# Contributing to Spring Batch

Here are some ways for you to get involved in the community:

* Get involved with the Spring community on the Spring Community Forums.  Please help out on the [forum](http://forum.spring.io/forum/spring-projects/batch) by responding to questions and joining the debate.
* Create [JIRA](https://jira.spring.io/browse/BATCH) tickets for bugs and new features and comment and vote on the ones that you are interested in.
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/).  If you want to contribute code this way, please familiarize yourself with the process oulined for contributing to Spring projects here: [Contributor Guidelines](https://github.com/SpringSource/spring-integration/wiki/Contributor-Guidelines).
* Watch for upcoming articles on Spring by [subscribing](feed://assets.spring.io/drupal/node/feed.xml) to spring.io

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup).  Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join the core team, and given the ability to merge pull requests.

# Code of Conduct
 This project adheres to the Contributor Covenant link:CODE_OF_CONDUCT.adoc[code of conduct]. By participating, you  are expected to uphold this code. Please report unacceptable behavior to spring-code-of-conduct@pivotal.io.
 