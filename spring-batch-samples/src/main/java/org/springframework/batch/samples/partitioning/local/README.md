## Local Partitioning Sample

### About

The purpose of this sample is to show multi-threaded step execution
using the `PartitionHandler` SPI.  The example uses a
`TaskExecutorPartitionHandler` to spread the work across multiple threads,
with one `Step` execution per thread.

For the XML sample, the configuration is in `/org/springframework/batch/samples/partition/jdbc/partitionJdbcJob.xml`.
The key components are the `PartitionStep` and the `MultiResourcePartitioner` which is responsible for dividing up
the work. 

For the Java sample, the configuration is in `org.springframework.batch.samples.partitioning.local.PartitionJdbcJobConfiguration`.
The key component is the `ColumnRangePartitioner` which is responsible for dividing up the work.

Notice that the readers and writers in the `Step` that is being partitioned are step-scoped, so that their state does not get shared across threads of execution.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=PartitionJdbcJobFunctionalTests#testUpdateCredit test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=PartitionJdbcJobFunctionalTests#testLaunchJobWithJavaConfiguration test
```