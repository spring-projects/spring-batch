# Issue reporting Guidelines

Thank you very much for taking the time to report a bug to us, we greatly appreciate it! This document is designed to allow Spring Batch users and team members to contribute self-contained projects containing [minimal complete verifiable examples](https://en.wikipedia.org/wiki/Minimal_reproducible_example) for issues logged against the [issue tracker](https://github.com/spring-projects/spring-batch/issues) on GitHub.

Our goal is to have a streamlined process for evaluating issues so bugs get fixed more quickly!

# Downloading the minimal complete verifiable example

We provide a minimal complete verifiable example that you can download here: [spring-batch-mcve.zip](https://raw.githubusercontent.com/wiki/spring-projects/spring-batch/mcve/spring-batch-mcve.zip). This example provides a starting point that you need to edit, zip and attach to your issue on GitHub. You need to use Java 17+ and Maven 3+.

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

# Reporting a bug

Once you have the minimal complete verifiable example running as expected, you can import it as a Maven project in your favourite IDE. Please make sure to:

* Update the sample as needed to reproduce your issue. We have placed a few TODOs where we expect you to modify the code.
* Add any dependency that is required to reproduce your issue.
* Keep the bare minimum code that is required to reproduce your issue. This is very important! Please reduce as much noise as possible to let us focus on the code related to the issue.

Once you manage to reproduce the issue, please clean up the `target` directory *before* creating the zip archive to upload. Here are the commands you can run to create the archive:

```shell
$>mvn clean
$>zip -r spring-batch-mcve.zip spring-batch-mcve
```

:exclamation: Important note: The `mvn clean` command is very important here. Please **DO NOT** include the `target` directory with all dependencies in the archive! We appreciate your collaboration on this.

Heads-up: If you think you can reproduce the issue with a JUnit test, that is awesome! The minimal example that we provide
has a JUnit test that you can edit as needed to reproduce the issue. Moreover, the minimal example we provide uses an in-memory H2 database, but if your issue is related to a specific database, please do not hesitate to add a Docker based test using
the [Testcontainers](https://www.testcontainers.org) library and the JDBC driver of your database. You can find several examples in the test suite of Spring Batch [here](https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/test/java/org/springframework/batch/core/test/repository/MySQLJobRepositoryIntegrationTests.java).

# Final thoughts

More importantly, put yourself in the shoes of the project maintainer who is in charge of analysing and trying to reproduce your issue. Before uploading your minimal example, ask yourself: "How fast the Spring Batch team can understand and reproduce my issue?"

Once we download your zip archive from the corresponding issue on GitHub, we expect to be two commands away from seeing a stack trace or the described abnormal behaviour:

```shell
$>unzip spring-batch-mcve.zip && cd spring-batch-mcve
$>mvn package exec:java -Dexec.mainClass=org.springframework.batch.MyBatchJobConfiguration
```

Finally, please remember that those instructions are guidelines and not hard requirements. Be pragmatic! For example, if you already have a GitHub repository with the minimal example, there is no need to zip it and attach it to the issue, you would just need to add a link to it in your issue. If you think the issue is really obvious and does not require a minimal example, there is no need to create such an example, just go ahead and create the issue on GitHub by following the [Issue Template](https://github.com/spring-projects/spring-batch/blob/main/.github/ISSUE_TEMPLATE/bug_report.md).

We appreciate your collaboration and we would like to thank you upfront for your time and effort!
