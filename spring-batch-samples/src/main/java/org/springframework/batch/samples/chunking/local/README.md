## Local Chunking Sample

### About

This sample shows how to use the `ChunkTaskExecutorItemWriter` to write items in a
multi-threaded way.  The example uses a `ThreadPoolTaskExecutor` to write items
in parallel with multiple threads, each one writing a chunk of items in its own transaction.

In this sample, data items of type `Vet` are read from a flat file and written to
a relational database table. The `Vet` class is a simple domain object representing a
veterinarian, with properties `firstName`, and `lastName`. The flat file contains records
of veterinarians, and each record is mapped to a `Vet` object using a `FlatFileItemReader`.
The `JdbcBatchItemWriter` is then used to persist these `Vet` objects into a relational database
table named `vets`. The table has columns corresponding to the properties of the `Vet` class.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=LocalChunkingJobFunctionalTests#testLaunchJobWithJavaConfig test
```