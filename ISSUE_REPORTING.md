# Issue Reporting Guidelines

Thank you very much for taking the time to report a bug to us, we greatly appreciate it! This document is designed to allow Spring Batch users and team members to contribute self-contained projects containing [minimal complete verifiable examples](https://en.wikipedia.org/wiki/Minimal_reproducible_example) for issues logged against the [issue tracker](https://github.com/spring-projects/spring-batch/issues) on GitHub.

Our goal is to have a streamlined process for evaluating issues so that bugs get fixed more quickly!

# How do I report a bug?

## 1. Download the template of a minimal complete verifiable example

We provide a template of a minimal complete verifiable example that you can download here: [spring-batch-mcve.zip](https://raw.githubusercontent.com/wiki/spring-projects/spring-batch/mcve/spring-batch-mcve.zip).
This example uses an in-memory H2 database and provides a starting point that you need to edit, zip and attach to your issue on GitHub. You need to use Java 17+ and Maven 3+.

Please run the following commands to make sure you have the sample working as expected:

```shell
$>unzip spring-batch-mcve.zip && cd spring-batch-mcve
$>mvn package exec:java -Dexec.mainClass=org.springframework.batch.MyBatchJobConfiguration
```

You should see something like the following output:

```
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.configuration.annotation.BatchRegistrar - Finished Spring Batch infrastructure beans configuration in 5 ms.
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory - Starting embedded database: url='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.repository.support.JobRepositoryFactoryBean - No database type set, using meta data indicating: H2
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor - No Micrometer observation registry found, defaulting to ObservationRegistry.NOOP
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor - No Micrometer observation registry found, defaulting to ObservationRegistry.NOOP
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.launch.support.SimpleJobLauncher - No TaskExecutor has been set, defaulting to synchronous executor.
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.launch.support.SimpleJobLauncher - Job: [SimpleJob: [name=job]] launched with the following parameters: [{}]
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.job.SimpleStepHandler - Executing step: [step]
hello world
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.step.AbstractStep - Step: [step] executed in 11ms
[org.springframework.batch.MyBatchJobConfiguration.main()] INFO org.springframework.batch.core.launch.support.SimpleJobLauncher - Job: [SimpleJob: [name=job]] completed with the following parameters: [{}] and the following status: [COMPLETED] in 34ms
COMPLETED
```

## 2. Edit the example as needed

Once you have the minimal complete verifiable example running as expected, you can import it as a Maven project in your favourite IDE. Please make sure to:

* Update the sample as needed to reproduce your issue. We have placed a few TODOs where we expect you to modify the code.
* Add any dependency that is required to reproduce your issue in the `pom.xml` file.
* Keep only the code that is required to reproduce your issue. This is very important! Please reduce as much noise as possible to let us focus on the code related to the issue.

## 3. Package the example and attach it to your issue

Once you manage to reproduce the issue, please clean up the `target` directory *before* creating the zip archive to upload. Here are the commands you can run to create the archive:

```shell
$>mvn clean
$>zip -r spring-batch-mcve.zip spring-batch-mcve
```

:exclamation: Important note: The `mvn clean` command is very important here. Please **DO NOT** include the `target` directory with all dependencies in the archive! We appreciate your collaboration on this.

Heads-up: If you think you can reproduce the issue with a JUnit test, that is awesome! The minimal example that we provide has a JUnit test that you can edit as needed to reproduce the issue.

# What if I use another database than H2?

If your issue is related to a specific database, please start with the same example as in the previous section and add a Docker-based test using the [Testcontainers](https://www.testcontainers.org) library and the JDBC driver of your database.

For example, if you use PostgreSQL, you might add the following dependencies to the `pom.xml` file:

```xml
<!-- PostgreSQL JDBC driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version> <!-- update the version if needed -->
</dependency>

<!-- Testcontainers module for PostgreSQL -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.17.6</version> <!-- update the version if needed -->
    <scope>test</scope>
</dependency>
```

Also, remember to remove the H2 dependency as well, to keep the example as minimal as possible even in terms of dependencies.

You can find several examples of Docker-based tests in the [test suite of Spring Batch](https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/test/java/org/springframework/batch/core/test/repository), and a specific example for PostgreSQL [here](https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/test/java/org/springframework/batch/core/test/repository/PostgreSQLJobRepositoryIntegrationTests.java).

# What if I use Spring Boot?

If you use Spring Boot, the best way to create a minimal example is to generate a project on [https://start.spring.io](https://start.spring.io).

Here is a quick link to generate a Maven-based Spring Boot application with Spring Batch and H2: [Sample project](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.0.4&packaging=jar&jvmVersion=17&groupId=com.example&artifactId=demo&name=demo&description=Demo%20project%20for%20Spring%20Boot&packageName=com.example.demo&dependencies=batch,h2).

You can also generate a project on the command line, for example with `cURL`:

```shell
$>curl https://start.spring.io/starter.tgz -d dependencies=batch,h2 -d type=maven-project -d baseDir=spring-batch-mcve | tar -xzvf -
```

Once you have downloaded the project, please follow the same steps as in the previous section (edit the sample, zip it without the dependencies, etc).

# Final thoughts

More importantly, put yourself in the shoes of the project maintainer who is in charge of analysing and trying to reproduce your issue. Before uploading your minimal example, ask yourself: "How fast the Spring Batch team can understand and reproduce my issue?"

Once we download your zip archive from the corresponding issue on GitHub, we expect to be two commands away from seeing a stack trace or the described abnormal behaviour:

```shell
$>unzip spring-batch-mcve.zip && cd spring-batch-mcve
$>mvn package exec:java -Dexec.mainClass=org.springframework.batch.MyBatchJobConfiguration
```

Finally, please remember that those instructions are guidelines and not hard requirements. Be pragmatic! For example, if you already have a GitHub repository with the minimal example, there is no need to zip it and attach it to the issue, you would just need to add a link to it in your issue. If you think the issue is really obvious and does not require a minimal example, there is no need to create such an example, just go ahead and create the issue on GitHub by following the [Issue Template](https://github.com/spring-projects/spring-batch/blob/main/.github/ISSUE_TEMPLATE/bug_report.md).

We appreciate your collaboration and we would like to thank you upfront for your time and effort!
