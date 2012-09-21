# Spring Batch 

Spring Batch is a lightweight, comprehensive batch framework designed to enable the development of robust batch applications vital for the daily operations of enterprise systems.  Spring Batch builds upon the productivity, POJO-based development approach, and general ease of use capabilities people have come to know from the [Spring Framework](https://github.com/SpringSource/spring-framework), while making it easy for developers to access and leverage more advanced enterprise services when necessary.

If you are looking for a runtime container for your Batch applications, or need a management console to view current and historic executions, take a look at [Spring Batch Admin](http://www.springsource.org/spring-batch-admin).  It is a set of services (Java, JSON, JMX) and an optional web UI that you can use to manage and monitor a Batch system.

# Building from Source

Clone the git repository using the URL on the Github home page:

    $ git clone git://github.com/SpringSource/spring-batch.git
    $ cd spring-batch

## Command Line
Use Maven 2.2 or 3.0, then on the command line:

    $ mvn install

or, the first time (to download the stuff that isn't in the Maven Central repository):

    $ mvn install -P bootstrap

## SpringSource Tool Suite (STS)
In STS (or any Eclipse distro or other IDE with Maven support), import the module directories as existing projects.  They should compile and the tests should run with no additional steps.

# Getting Started Using SpringSource Tool Suite (STS)

  This is the quickest way to get started.  It requires an internet connection for download, and access to a Maven repository (remote or local).

* Download STS version 2.5.* (or better) from the [SpringSource website](http://www.springsource.com/products/sts).  STS is a free Eclipse bundle with many features useful for Spring developers.
* Go to `File->New->Spring Template Project` from the menu bar (in the Spring perspective).
* The wizard has a drop down with a list of template projects.  One of them is a "Simple Spring Batch Project".  Select it and follow the wizard.
* A project is created with all dependencies and a simple input/output job configuration.  It can be run using a unit test, or on the command line (see instructions in the pom.xml).

# Getting Help

Read the main project [website](http://www.springsource.org/spring-batch) and the [User Guide](http://www.springsource.org/spring-batch/reference). Look at the source code and the Javadocs.  For more detailed questions, use the [forum](http://forum.springsource.org/forumdisplay.php?f=41).  If you are new to Spring as well as to Spring Batch, look for information about [Spring projects](http://www.springsource.org/projects).

# Contributing to Spring Batch

Here are some ways for you to get involved in the community:

* Get involved with the Spring community on the Spring Community Forums.  Please help out on the [forum](http://forum.springsource.org/forumdisplay.php?f=41) by responding to questions and joining the debate.
* Create [JIRA](https://jira.springsource.org/browse/BATCH) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/).  If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://www.springsource.org/node/feed) to springframework.org

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup).  Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join the core team, and given the ability to merge pull requests.


# Getting Started Using the Samples

A convenient way to get started quickly with Spring Batch is to run the samples which are packaged in the samples module.  There is also a simple command line sample (or "archetype") which has a bare bones but complete implementation of a simpel job.  The source code for the samples (and the other modules) is available either from the [Zip assembly](http://www.springsource.org/spring-batch/downloads.html) or from [Git](http://www.springsource.org/spring-batch/source-repository.html).

## Using the .Zip Distribution

### With Maven and Eclipse

* Download the "no-dependencies" version of the distribution and unzip to create a directory `org.springframework.batch-<versionId`>.
* Get the [m2eclipse plugin](http://m2eclipse.sonatype.org/update)
  (installed in STS out of the box).  If you can't or don't want to
  install this plugin, you can use the [Maven Eclipse
  Plugin](http://maven.apache.org/plugins/maven-eclipse-plugin) to
  create the classpath entries you need.
* Open Eclipse and create a workspace as for the non-Mavenized version.
* Import the samples and archetype projects from the samples sub-directory in the directory you just unpacked.
* The project should build cleanly without having to fix the dependencies.  If it doesn't, make sure you are online, and maybe try building on the command line first to make sure all the dependencies are downloaded.  See the [build instructions](http://www.springsource.org/spring-batch/building.html) if you run into difficulty.

(N.B. the "archetype" is not a real Maven archetype, just a template project that can be used as a starting point for a self-contained batch job.  It is the same project that can be imported into STS using the Project Template wizard.)

### With Maven on the Command Line

* Download the distribution as above.
* Then run Maven in the spring-batch-samples directory, e.g.

            $ cd spring-batch-samples
            $ mvn test
            ...

### With Eclipse and without Maven

Similar instructions would apply to other IDEs.

* Download the "no-dependencies" package and unzip to create a directory `spring-batch-<versionId`>.
* Open Eclipse and make a workspace in the directory you just created.
* Import the `org.springframework.batch.samples` project from the samples directory.
* Find all the compile scope and non-optional runtime jar files listed in the [core dependencies report](http://www.springsource.org/spring-batch/spring-batch-core/dependencies.html) and [infrastructure dependencies](http://www.springsource.org/spring-batch/spring-batch-infrastructure/dependencies.html), and import them into the project.
* Force the workspace to build (e.g. Project -> Clean...)
* Run the unit tests in your project under src/test/java.  N.B. the FootbalJobFunctionTests takes quite a long time to run.

You can get a pretty good idea about how to set up a job by examining the unit tests in the `org.springframework.batch.sample` package (in `src/main/java`) and the configuration in `src/main/resources/jobs`.

To launch a job from the command line instead of a unit test use the `CommandLineJobRunner.main()` method (see Javadocs included in that class).

## Using Maven and Git

* Check out the Spring Batch project from Git (instructions are available [here](https://github.com/SpringSource/spring-batch)).
* Run Maven from the command line in the samples directory.  There are additional building instructions and suggestions about what to do if it goes wrong [here](http://www.springsource.org/spring-batch/building.html).
