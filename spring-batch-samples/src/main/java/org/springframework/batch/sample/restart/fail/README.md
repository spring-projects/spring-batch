## Fail / Restart Sample

### About

The goal of this sample is to show how a job can be restarted after
a failure and continue processing where it left off.

To simulate a failure we "fake" a failure on the fourth record
though the use of a sample component
`ExceptionThrowingItemReaderProxy`.  This is a stateful reader
that counts how many records it has processed and throws a planned
exception in a specified place.  Since we re-use the same instance
when we restart the job it will not fail the second time.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=RestartFunctionalTests#testLaunchJob test
```