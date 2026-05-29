## Stop / Restart Sample

### About

This sample has a single step that is an infinite loop, reading and
writing fake data.  It is used to demonstrate stop signals and
restart capabilities.

### Run the sample

For a command-line stop and restart walkthrough, use the
[Graceful Shutdown sample](../../shutdown/README.md). It provides executable
entry points to start, stop, and restart a job:

```
$>./mvnw -pl org.springframework.batch:spring-batch-samples exec:java -Dexec.mainClass=org.springframework.batch.samples.shutdown.StartJobExecutionApp
$>./mvnw -pl org.springframework.batch:spring-batch-samples exec:java -Dexec.mainClass=org.springframework.batch.samples.shutdown.StopJobExecutionApp
$>./mvnw -pl org.springframework.batch:spring-batch-samples exec:java -Dexec.mainClass=org.springframework.batch.samples.shutdown.RestartJobExecutionApp
```
