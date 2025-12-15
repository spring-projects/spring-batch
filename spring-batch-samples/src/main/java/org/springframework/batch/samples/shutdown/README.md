# Graceful Shutdown sample

## About the sample

This sample demonstrates how to implement a graceful shutdown in a Spring Batch application.
It includes a job that simulates a long-running task and a shutdown hook that ensures the job
is stopped gracefully when the application receives a termination signal.

The job consists of a single step that processes a large number of items, simulating a long-running task.
The shutdown hook listens for termination signals (e.g., SIGTERM) and attempts to stop the job execution
gracefully, allowing it to complete its current processing before shutting down.

## Run the sample

### 1. Start the job

First, you need to start the database server of the job repository. 

Open a terminal and run the following command from the top level directory of the project:

```
$>docker-compose -f spring-batch-samples/src/main/resources/org/springframework/batch/samples/shutdown/docker-compose.yml up -d
```

Then, run the `org.springframework.batch.samples.shutdown.StartJobExecutionApp` class in your IDE with no arguments.

You can also run the application from the command line using this command:

```bash
./mvnw -pl org.springframework.batch:spring-batch-samples exec:java -Dexec.mainClass=org.springframework.batch.samples.shutdown.StartJobExecutionApp
```

Get the process id from the first line of logs (needed for the stop):

```
Process id = 73280
```

### 2. Stop the job

In a second terminal, Gracefully stop the app by running:

```
$>kill -15 73280
```

You can otherwise run the `org.springframework.batch.samples.shutdown.StopJobExecutionApp` class in your IDE with no arguments.

You can also stop the execution on the command line with the following command:

```bash
./mvnw -pl org.springframework.batch:spring-batch-samples exec:java -Dexec.mainClass=org.springframework.batch.samples.shutdown.StopJobExecutionApp
```

You should see the shutdown hook being called:

```
Received JVM shutdown signal
Attempting to gracefully stop job execution 1
[...]
Successfully stopped job execution 1
```

Now, check the job execution status in the database with the following commands:

```
$>docker exec postgres psql -U postgres -c 'select * from batch_job_execution;'
$>docker exec postgres psql -U postgres -c 'select * from batch_step_execution;'
```

Both the job and the step should have a `STOPPED` status.

### 3. Restart the job

Now, you can restart the job by running the `org.springframework.batch.samples.shutdown.RestartJobExecutionApp` class in your IDE.

Or restart the execution on the command line with the following command:

```bash
./mvnw -pl org.springframework.batch:spring-batch-samples exec:java -Dexec.mainClass=org.springframework.batch.samples.shutdown.RestartJobExecutionApp
```

You should see that the job is restarted from the last commit point and completes successfully.

## Clean up

To stop the database server, run:

```
$>docker-compose -f spring-batch-samples/src/main/resources/org/springframework/batch/samples/shutdown/docker-compose.yml down
```