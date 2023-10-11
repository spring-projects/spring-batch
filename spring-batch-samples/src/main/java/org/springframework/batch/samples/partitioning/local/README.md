## Local Partitioning Sample

### About

The purpose of this sample is to show multi-threaded step execution
using the `PartitionHandler` SPI.  The example uses a
`TaskExecutorPartitionHandler` to spread the work of reading
some files across multiple threads, with one `Step` execution
per thread.  The key components are the `PartitionStep` and the
`MultiResourcePartitioner` which is responsible for dividing up
the work.  Notice that the readers and writers in the `Step`
that is being partitioned are step-scoped, so that their state does
not get shared across threads of execution.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=MailJobFunctionalTests#testLaunchJob test
```