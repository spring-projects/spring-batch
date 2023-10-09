## Process Indicator pattern Sample

## About

The purpose of this sample is to show multi-threaded step execution
using the Process Indicator pattern.

The job reads data from the a fixed length file , but instead of
writing it out directly it goes through a staging table, and the
staging table is read in a multi-threaded step.  Note that for such
a simple example where the item processing was not expensive, there
is unlikely to be much if any benefit in using a multi-threaded
step.

Multi-threaded step execution is easy to configure using Spring
Batch, but there are some limitations.  Most of the out-of-the-box
`ItemReader` and `ItemWriter` implementations are not
designed to work in this scenario because they need to be
restartable and they are also stateful.  There should be no surprise
about this, and reading a file (for instance) is usually fast enough
that multi-threading that part of the process is not likely to
provide much benefit, compared to the cost of managing the state.

The best strategy to cope with restart state from multiple
concurrent threads depends on the kind of input source involved:

* For file-based input (and output) restart sate is practically
  impossible to manage.  Spring Batch does not provide any features
  or samples to help with this use case.
* With message middleware input it is trivial to manage restarts,
  since there is no state to store (if a transaction rolls back the
  messages are returned to the destination they came from).
* With database input state management is still necessary, but it
  isn't particularly difficult.  The easiest thing to do is rely on
  a Process Indicator in the input data, which is a column in the
  data indicating for each row if it has been processed or not.  The
  flag is updated inside the batch transaction, and then in the case
  of a failure the updates are lost, and the records will show as
  un-processed on a restart.

This last strategy is implemented in the `StagingItemReader`.
Its companion, the `StagingItemWriter` is responsible for
setting up the data in a staging table which contains the process
indicator.  The reader is then driven by a simple SQL query that
includes a where clause for the processed flag, i.e.

```sql
SELECT ID FROM BATCH_STAGING WHERE JOB_ID=? AND PROCESSED=? ORDER BY ID
```

It is then responsible for updating the processed flag (which
happens inside the main step transaction).

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=ProcessIndicatorJobFunctionalTests#testLaunchJob test
```