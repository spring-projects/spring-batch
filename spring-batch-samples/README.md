## Spring Batch Samples

### Overview

There is considerable variability in the types of input and output
formats in batch jobs. There are also a number of options to consider
in terms of how the types of strategies that will be used to handle
skips, recovery, and statistics. However, when approaching a new
batch job there are a few standard questions to answer to help
determine how the job will be written and how to use the services
offered by the spring batch framework. Consider the following:

* How do I configure this batch job? In the samples the pattern is to follow the convention of `[nameOf]Job.xml`. Each sample identifies the XML definition used to configure the job. Job configurations that use a common execution environment have many common items in their respective configurations.
* What is the input source? Each sample batch job identifies its input source.
* What is my output source? Each sample batch job identifies its output source.
* How are records read and validated from the input source? This refers to the input type and its format (e.g. flat file with fixed position, comma separated or XML, etc.)
* What is the policy of the job if a input record fails the validation step? The most important aspect is whether the record can be skipped so that processing can be continued.
* How do I process the data and write to the output source? How and what business logic is being applied to the processing of a record?
* How do I recover from an exception while operating on the output source? There are numerous recovery strategies that can be applied to handling errors on transactional targets. The samples provide a feeling for some of the choices.
* Can I restart the job and if so which strategy can I use to restart the job? The samples show some of the options available to jobs and what the decision criteria is for the respective choices.

Here is a list of samples with checks to indicate which features each one demonstrates:

| Job/Feature                                                   | skip | retry | restart | automatic mapping | asynch launch | validation | delegation | write behind | non-sequential | asynch process | filtering |
|:--------------------------------------------------------------|:----:|:-----:|:-------:|:-----------------:|:-------------:|:----------:|:----------:|:------------:|:--------------:|:--------------:|:---------:|
| [Hello world Job Sample](#hello-world-job-sample)             |      |       |         |                   |               |            |            |              |                |       X        |           |
| [Amqp Job Sample](#amqp-job-sample)                           |      |       |         |                   |               |            |            |              |                |       X        |           |
| [BeanWrapperMapper Sample](#beanwrappermapper-sample)         |      |       |         |         X         |               |            |            |              |                |                |           |
| [Composite ItemReader Sample](#composite-itemreader-sample)        |      |       |         |                   |               |            |     X      |              |                |                |           |
| [Composite ItemWriter Sample](#composite-itemwriter-sample)   |      |       |         |                   |               |            |     X      |              |                |                |           |
| [Customer Filter Sample](#customer-filter-sample)             |      |       |         |                   |               |            |            |              |                |                |     X     |
| [Reader Writer Adapter Sample](#reader-writer-adapter-sample) |      |       |         |                   |               |            |     X      |              |                |                |           |
| [Football Job](#football-job)                                 |      |       |         |                   |               |            |            |              |                |                |           |
| [Trade Job](#trade-job)                                       |      |       |         |                   |               |     X      |            |              |                |                |           |
| [Header Footer Sample](#header-footer-sample)                 |      |       |         |                   |               |            |            |              |                |                |           |
| [Loop Flow Sample](#loop-flow-sample)                         |      |       |         |                   |               |            |            |              |                |                |           |
| [Multiline Sample](#multiline-input-job)                      |      |       |         |                   |               |            |     X      |              |                |                |           |
| [Pattern matching Sample](#pattern-matching-sample)           |      |       |         |                   |               |            |     X      |              |                |                |           |
| [Process indicator Sample](#process-indicator-pattern-sample) |      |       |         |                   |               |            |            |              |                |       X        |           |
| [Local Partitioning Sample](#local-partitioning-sample)       |      |       |         |                   |               |            |            |              |                |       X        |           |
| [Remote Partitioning Sample](#remote-partitioning-sample)     |      |       |         |                   |               |            |            |              |                |       X        |           |
| [Remote Chunking Sample](#remote-chunking-sample)             |      |       |         |                   |               |            |            |              |                |       X        |           |
| [Quartz Sample](#quartz-sample)                               |      |       |         |                   |       X       |            |            |              |                |                |           |
| [Stop Restart Sample](#stop-restart-sample)                   |      |       |    X    |                   |               |            |            |              |                |                |           |
| [Fail Restart Sample](#fail-restart-sample)                   |      |       |    X    |                   |               |            |            |              |                |                |           |
| [Retry Sample](#retry-sample)                                 |      |   X   |         |                   |               |            |            |              |                |                |           |
| [Skip Sample](#skip-sample)                                   |  X   |       |         |                   |               |            |            |              |                |                |           |
| [Chunk Scanning Sample](#chunk-scanning-sample)               |  X   |   X   |         |                   |               |            |            |              |                |                |           |
| [Adhoc Loop and JMX Demo](#adhoc-loop-and-jmx-sample)         |      |       |    X    |                   |       X       |            |            |              |                |                |           |

The IO Sample Job has a number of special instances that show different IO features using the same job configuration but with different readers and writers:

| Job/Feature                                                         | delimited input | fixed-length input | xml input | db paging input | db cursor input | delimited output | fixed-length output | xml output | db output | multiple files | multi-line | multi-record |
|:--------------------------------------------------------------------|:---------------:|:------------------:|:---------:|:---------------:|:---------------:|:----------------:|:-------------------:|:----------:|:---------:|:--------------:|:----------:|:------------:|
| [Delimited File Import Job](#delimited-file-import-job)             |        x        |                    |           |                 |                 |                  |                     |     x      |           |                |            |              |
| [Fixed Length Import Job](#fixed-length-import-job)                 |                 |         x          |           |                 |                 |                  |                     |            |     x     |                |            |              |
| [Jdbc Readers and Writers Sample](#jdbc-readers-and-writers-sample) |                 |                    |           |                 |        x        |                  |                     |            |           |                |     x      |              |
| [JPA Readers and Writers sample](#jpa-readers-and-writers-sample)   |                 |                    |           |        x        |                 |                  |                     |            |           |                |     x      |              |
| [Multiline Input Sample](#multiline-input-job)                      |        x        |                    |           |                 |                 |                  |                     |     x      |           |                |     x      |              |
| [multiRecord Type Sample](#multirecord-type-input-job)              |                 |         x          |           |                 |                 |                  |                     |            |     x     |                |            |      x       |
| [multiResource Sample](#multiresource-input-output-job)             |        x        |                    |           |                 |                 |                  |                     |     x      |           |       x        |            |      x       |
| [XML Input Output Sample](#xml-input-output)                        |                 |                    |     x     |                 |                 |                  |                     |            |           |                |            |              |
| [MongoDB sample](#mongodb-sample)                                   |                 |                    |           |                 |        x        |                  |                     |            |     x     |                |            |              |
| [PetClinic sample](#petclinic-sample)                               |                 |                    |           |                 |        x        |        x         |                     |            |           |                |            |              |

### Common Sample Source Structures

Samples are organised by feature in separate packages. Each sample
has a specific README file in its corresponding package.

The easiest way to launch a sample is to open up a unit test in
your IDE and run it directly.  Each sample has a test case in the
`org.springframework.batch.samples` package. The name of the test
case is `[JobName]FunctionalTests`. You can also run each sample 
from the command line as follows:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=[JobName]FunctionalTests#test[JobName] test
```

Please refer to the README of each sample for launching instructions.

### Hello world Job sample

This sample is a single-step job that prints "Hello world!" to the standard
output. It shows the basic setup to configure and use Spring Batch.

[Hello world sample](src/main/java/org/springframework/batch/samples/helloworld/README.md)

### Jdbc Readers and Writers sample

The purpose of this sample is to show to usage of the
`JdbcCursorItemReader`/`JdbcPagingItemReader` and the `JdbcBatchItemWriter` to make
efficient updates to a database table.

[Jdbc Readers and Batch Update sample](src/main/java/org/springframework/batch/samples/jdbc/README.md)

### JPA Readers and Writers sample

The purpose of this sample is to show to usage of the JPA item readers and writers
to read and write data from/to a database with JPA and Hibernate.

[JPA Readers and Writers sample](src/main/java/org/springframework/batch/samples/jpa/README.md)

### Amqp Job Sample

This sample shows the use of Spring Batch to write to an `AmqpItemWriter`.
The `AmqpItemReader` and Writer were contributed by Chris Schaefer.
It is modeled after the `JmsItemReader` / Writer implementations, which
are popular models for remote chunking. It leverages the `AmqpTemplate`.

[Amqp Job Sample](src/main/java/org/springframework/batch/samples/amqp/README.md)

### BeanWrapperMapper Sample

This sample shows the use of automatic mapping from fields in a file
to a domain object.  The `Trade` and `Person` objects needed
by the job are created from the Spring configuration using prototype
beans, and then their properties are set using the
`BeanWrapperFieldSetMapper`, which sets properties of the
prototype according to the field names in the file.

[BeanWrapperMapper Sample](src/main/java/org/springframework/batch/samples/beanwrapper/README.md)

### Composite ItemReader Sample

This sample shows how to use a composite item reader to read data with
the same format from different data sources.

In this sample, data items of type `Person` are read from two flat files
and a relational database table.

[Composite reader Sample](src/main/java/org/springframework/batch/samples/compositereader/README.md)

### Composite ItemWriter Sample

This shows a common use case using a composite pattern, composing
instances of other framework readers or writers.  It is also quite
common for business-specific readers or writers to wrap
off-the-shelf components in a similar way.

In this job the composite pattern is used just to make duplicate
copies of the output data.  The delegates for the
`CompositeItemWriter` have to be separately registered as
streams in the `Step` where they are used, in order for the step
to be restartable.  This is a common feature of all delegate
patterns.

[Composite writer Sample](src/main/java/org/springframework/batch/samples/compositewriter/README.md)

### Customer Filter Sample

This shows the use of the `ItemProcessor` to filter out items by
returning null.  When an item is filtered it leads to an increment
in the `filterCount` in the step execution.

[Customer Filter Sample](src/main/java/org/springframework/batch/samples/filter/README.md)

### Reader Writer Adapter Sample

This sample shows the delegate pattern, and also the
`ItemReaderAdapter` which is used to adapt a POJO to the
`ItemReader` interface.

[Reader Writer Adapter Sample](src/main/java/org/springframework/batch/samples/adapter/readerwriter/README.md)

### Tasklet Adapter Sample

This sample shows the delegate pattern again, to adapt an
existing service to a `Tasklet`.

[Tasklet Adapter Sample](src/main/java/org/springframework/batch/samples/adapter/tasklet/README.md)

### Delimited File Import Job

The goal is to demonstrate a typical scenario of reading data
from a delimited file, processing it and writing it to another file.

[Delimited file Job sample](src/main/java/org/springframework/batch/samples/file/delimited/README.md)

### Fixed Length Import Job

The goal is to demonstrate a typical scenario of reading data
from a fixed length file, processing it and writing it to another file.

[Fixed Length Import Job sample](src/main/java/org/springframework/batch/samples/file/fixed/README.md)

### XML Input Output

The goal here is to show the use of XML input and output through
streaming and Spring OXM marshallers and unmarshallers.

[XML Input Output](src/main/java/org/springframework/batch/samples/file/xml/README.md)

### JSON Input Output

The goal of this sample is to show how to read and write JSON files:

```json
[
  {"isin":"123","quantity":5,"price":10.5,"customer":"foo","id":1,"version":0},
  {"isin":"456","quantity":10,"price":20.5,"customer":"bar","id":2,"version":0},
  {"isin":"789","quantity":15,"price":30.5,"customer":"baz","id":3,"version":0}
]
```

[JSON Input Output](src/main/java/org/springframework/batch/samples/file/json/README.md)

### MultiResource Input Output Job

This sample shows how to use the `MultiResourceItemReader` and `MultiResourceItemWriter`
to read and write multiple files in the same step.

[MultiResource Input Output Job Sample](src/main/java/org/springframework/batch/samples/file/multiresource/README.md)

### MultiLine Input Job

The goal of this sample is to show how to process input files where a single logical
item spans multiple physical line:

```
BEGIN
INFO,UK21341EAH45,customer1
AMNT,978,98.34
END
BEGIN
INFO,UK21341EAH46,customer2
AMNT,112,18.12
END
...
```

[MultiLine Input Job Sample](src/main/java/org/springframework/batch/samples/file/multiline/README.md)

### MultiRecord type Input Job

The goal of this sample is to show how to use the `PatternMatchingCompositeLineMapper` API
to process files containing lines of different types:

```
CUST42001customer100012000
CUST42002customer200022000
CUST42003customer300032000
TRADUK21341EAH45978 98.34customer1
TRADUK21341EAH46112 18.12customer2
CUST42004customer400042000
CUST42005customer500052000
TRADUK21341EAH47245 12.78customer3
TRADUK21341EAH48108109.25customer4
TRADUK21341EAH49854123.39customer5
CUST42006customer600062000
TRADUK21341EAH50234 32.45customer6
...
```

[MultiRecord type Input Job Sample](src/main/java/org/springframework/batch/samples/file/multirecordtype/README.md)

### Multiline Aggregate Sample

The goal of this sample is to show some common tricks with multiline
records in file input jobs.

The input file in this case consists of two groups of trades
delimited by special lines in a file (BEGIN and END):

```
BEGIN
UK21341EAH4597898.34customer1
UK21341EAH4611218.12customer2
END
BEGIN
UK21341EAH4724512.78customer2
UK21341EAH4810809.25customer3
UK21341EAH4985423.39customer4
END
```

The goal of the job is to operate on the two groups, so the item
type is naturally `List<Trade`>.

[Multiline Aggregate Sample](src/main/java/org/springframework/batch/samples/file/multilineaggregate/README.md)

### Pattern Matching Sample

The goal is to demonstrate how to handle a more complex file input
format, where a record meant for processing includes nested records
and spans multiple lines.

The input source is a file with multiline records:

```
HEA;0013100345;2007-02-15
NCU;Smith;Peter;;T;20014539;F
BAD;;Oak Street 31/A;;Small Town;00235;IL;US
SAD;Smith, Elizabeth;Elm Street 17;;Some City;30011;FL;United States
BIN;VISA;VISA-12345678903
LIT;1044391041;37.49;0;0;4.99;2.99;1;45.47
LIT;2134776319;221.99;5;0;7.99;2.99;1;221.87
SIN;UPS;EXP;DELIVER ONLY ON WEEKDAYS
FOT;2;2;267.34
HEA;0013100346;2007-02-15
BCU;Acme Factory of England;72155919;T
BAD;;St. Andrews Road 31;;London;55342;;UK
BIN;AMEX;AMEX-72345678903
LIT;1044319101;1070.50;5;0;7.99;2.99;12;12335.46
LIT;2134727219;21.79;5;0;7.99;2.99;12;380.17
LIT;1044339301;79.95;0;5.5;4.99;2.99;4;329.72
LIT;2134747319;55.29;10;0;7.99;2.99;6;364.45
LIT;1044359501;339.99;10;0;7.99;2.99;2;633.94
SIN;FEDX;AMS;
FOT;5;36;14043.74
```

[Pattern Matching Sample](src/main/java/org/springframework/batch/samples/file/patternmatching/README.md)

### Football Job

This is a (American) Football statistics loading job. It loads two files containing players and games
data into a database, and then combines them to summarise how each player performed for a particular year.

[Football Job](src/main/java/org/springframework/batch/samples/football/README.md)

### Trade Job

The goal is to show a reasonably complex scenario, that would
resemble the real-life usage of the framework.

This job has 3 steps.  First, data about trades are imported from a
file to database. Second, the trades are read from the database and
credit on customer accounts is decreased appropriately. Last, a
report about customers is exported to a file.

[Trade Job](src/main/java/org/springframework/batch/samples/trade/README.md)

### Header Footer Sample

This sample shows the use of callbacks and listeners to deal with
headers and footers in flat files.  It uses two custom callbacks:

* `HeaderCopyCallback`: copies the header of a file from the
input to the output.
* `SummaryFooterCallback`: creates a summary footer at the end
of the output file.

[Header Footer Sample](src/main/java/org/springframework/batch/samples/headerfooter/README.md)

### Stop Restart Sample

This sample has a single step that is an infinite loop, reading and
writing fake data.  It is used to demonstrate stop signals and
restart capabilities.

[Stop / Restart Sample](src/main/java/org/springframework/batch/samples/restart/stop/README.md)

### Fail Restart Sample

The goal of this sample is to show how a job can be restarted after
a failure and continue processing where it left off.

To simulate a failure we "fake" a failure on the fourth record
though the use of a sample component
`ExceptionThrowingItemReaderProxy`.  This is a stateful reader
that counts how many records it has processed and throws a planned
exception in a specified place.  Since we re-use the same instance
when we restart the job it will not fail the second time.

[Fail / Restart Sample](src/main/java/org/springframework/batch/samples/restart/fail/README.md)

### Loop Flow Sample

Shows how to implement a job that repeats one of its steps up to a
limit set by a `JobExecutionDecider`.

[Loop Flow Sample](src/main/java/org/springframework/batch/samples/loop/README.md)

### Process Indicator pattern Sample

The purpose of this sample is to show multi-threaded step execution
using the Process Indicator pattern.

The job reads data from the same file as the [Fixed Length Import sample](#fixed-length-import-job), but instead of
writing it out directly it goes through a staging table, and the
staging table is read in a multi-threaded step.

[Process Indicator pattern Sample](src/main/java/org/springframework/batch/samples/processindicator/README.md)

### Local Partitioning Sample

The purpose of this sample is to show multi-threaded step execution
using the `PartitionHandler` SPI.  The example uses a
`TaskExecutorPartitionHandler` to spread the work of reading
some files across multiple threads, with one `Step` execution
per thread.  The key components are the `PartitionStep` and the
`MultiResourcePartitioner` which is responsible for dividing up
the work.  Notice that the readers and writers in the `Step`
that is being partitioned are step-scoped, so that their state does
not get shared across threads of execution.

### Remote Partitioning Sample

This sample shows how to configure a remote partitioning job. The manager step
uses a `MessageChannelPartitionHandler` to send partitions to and receive 
replies from workers. Two examples are shown:

* A manager step that polls the job repository to see if all workers have finished
their work
* A manager step that aggregates replies from workers to notify work completion

The sample uses an embedded JMS broker and an embedded database for simplicity
but any option supported via Spring Integration for communication is technically
acceptable. 

### Remote Chunking Sample

This sample shows how to configure a remote chunking job. The manager step will
read numbers from 1 to 6 and send two chunks ({1, 2, 3} and {4, 5, 6}) to workers
for processing and writing.

This example shows how to use:

* the `RemoteChunkingManagerStepBuilderFactory` to create a manager step
* the `RemoteChunkingWorkerBuilder` to configure an integration flow on the worker side.

The sample uses an embedded JMS broker as a communication middleware between the
manager and workers. The usage of an embedded broker is only for simplicity's sake,
the communication between the manager and workers is still done through JMS queues
and Spring Integration channels and messages are sent over the wire through a TCP port.

### Quartz Sample

The goal is to demonstrate how to schedule job execution using
Quartz scheduler.

[Quartz Sample](src/main/java/org/springframework/batch/samples/misc/quartz/README.md)

### Retry Sample

The purpose of this sample is to show how to use the automatic retry
capabilities of Spring Batch.

The retry is configured in the step through the
`SkipLimitStepFactoryBean`:

```xml
<bean id="step1" parent="simpleStep"
    class="org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean">
    ...
    <property name="retryLimit" value="3" />
    <property name="retryableExceptionClasses" value="java.lang.Exception" />
</bean>
```

Failed items will cause a rollback for all `Exception` types, up
to a limit of 3 attempts.  On the 4th attempt, the failed item would
be skipped, and there would be a callback to a
`ItemSkipListener` if one was provided (via the "listeners"
property of the step factory bean).

An `ItemReader` is provided that will generate unique
`Trade` data by just incrementing a counter.  Note that it uses
the counter in its `mark()` and `reset()` methods so that
the same content is returned after a rollback.  The same content is
returned, but the instance of `Trade` is different, which means
that the implementation of `equals()` in the `Trade` object
is important.  This is because to identify a failed item on retry
(so that the number of attempts can be counted) the framework by
default uses `Object.equals()` to compare the recently failed
item with a cache of previously failed items.  Without implementing
a field-based `equals()` method for the domain object, our job
will spin round the retry for potentially quite a long time before
failing because the default implementation of `equals()` is
based on object reference, not on field content.

### Skip Sample

The purpose of this sample is to show how to use the skip features
of Spring Batch.  Since skip is really just a special case of retry
(with limit 0), the details are quite similar to the [Retry
Sample](#retry-sample), but the use case is less artificial, since it
is based on the [Trade Sample](#trade-job).

The failure condition is still artificial, since it is triggered by
a special `ItemWriter` wrapper (`ItemTrackingItemWriter`).
The plan is that a certain item (the third) will fail business
validation in the writer, and the system can then respond by
skipping it.  We also configure the step so that it will not roll
back on the validation exception, since we know that it didn't
invalidate the transaction, only the item.  This is done through the
transaction attribute:

```xml

<bean id="step2" parent="skipLimitStep">
    <property name="skipLimit" value="1"/>
    <!-- No rollback for exceptions that are marked with "+" in the tx attributes -->
    <property name="transactionAttribute"
              value="+org.springframework.batch.infrastructure.item.validator.ValidationException"/>
    ....
</bean>
```

The format for the transaction attribute specification is given in
the Spring Core documentation (e.g. see the Javadocs for
[TransactionAttributeEditor](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/transaction/interceptor/TransactionAttributeEditor.html)).

### Chunk Scanning Sample

In a fault tolerant chunk-oriented step, when a skippable exception is thrown during
item writing, the item writer (which receives a chunk of items) does not 
know which item caused the issue. Hence, it will "scan" the chunk item by item 
and only the faulty item will be skipped. Technically, the commit-interval will 
be re-set to 1 and each item will re-processed/re-written in its own transaction.

The `org.springframework.batch.samples.skip.SkippableExceptionDuringWriteSample` sample
illustrates this behaviour:

* It reads numbers from 1 to 6 in chunks of 3 items, so two chunks are created: [1, 2 ,3] and [4, 5, 6]
* It processes each item by printing it to the standard output and returning it as is.
* It writes items to the standard output and throws an exception for item 5

The expected behaviour when an exception occurs at item 5 is that the second chunk [4, 5, 6] is
scanned item by item. Transactions of items 4 and 6 will be successfully committed, while
the one of item 5 will be rolled back. Here is the output of the sample with some useful comments:

```
1.  reading item = 1
2.  reading item = 2
3.  reading item = 3
4.  processing item = 1
5.  processing item = 2
6.  processing item = 3
7.  About to write chunk: [1, 2, 3]
8.  writing item = 1
9.  writing item = 2
10. writing item = 3
11. reading item = 4
12. reading item = 5
13. reading item = 6
14. processing item = 4
15. processing item = 5
16. processing item = 6
17. About to write chunk: [4, 5, 6]
18. writing item = 4
19. Throwing exception on item 5
20. processing item = 4
21. About to write chunk: [4]
22. writing item = 4
23. processing item = 5
24. About to write chunk: [5]
25. Throwing exception on item 5
26. processing item = 6
27. About to write chunk: [6]
28. writing item = 6
29. reading item = null
```

* Lines 1-10: The first chunk is processed without any issue
* Lines 11-17: The second chunk is read and processed correctly and is about to be written
* Line 18: Item 4 is successfully written
* Line 19: An exception is thrown when attempting to write item 5, the transaction is rolled back and chunk scanning is about to start
* Lines 20-22: Item 4 is re-processed/re-written successfully in its own transaction
* Lines 23-25: Item 5 is re-processed/re-written with an exception. Its transaction is rolled back and is skipped
* Lines 26-28: Item 6 is re-processed/re-written successfully in its own transaction
* Line 29: Attempting to read the next chunk, but the reader returns `null`:
the datasource is exhausted and the step ends here

Similar examples show the expected behaviour when a skippable exception is thrown
during reading and processing can be found in
`org.springframework.batch.samples.skip.SkippableExceptionDuringReadSample`
and `org.springframework.batch.samples.skip.SkippableExceptionDuringProcessSample`.

### Batch metrics with Micrometer

This sample shows how to use [Micrometer](https://micrometer.io) to collect batch metrics in Spring Batch.
It uses [Prometheus](https://prometheus.io) as the metrics back end and [Grafana](https://grafana.com) as the front end. 
The sample consists of two jobs:

* `job1` : Composed of two tasklets that print `hello` and `world`
* `job2` : Composed of single chunk-oriented step that reads and writes a random number of items

These two jobs are run repeatedly at regular intervals and might fail randomly for demonstration purposes.

This sample requires [docker compose](https://docs.docker.com/compose/) to start the monitoring stack.
To run the sample, please follow these steps:

```
$>cd spring-batch-samples/src/main/resources/org/springframework/batch/samples/metrics
$>docker-compose up -d
```

This should start the required monitoring stack:

* Prometheus server on port `9090`
* Prometheus push gateway on port `9091`
* Grafana on port `3000`

Once started, you need to [configure Prometheus as data source in Grafana](https://grafana.com/docs/grafana/latest/datasources/prometheus/configure/)
and import the ready-to-use dashboard in `spring-batch-samples/src/main/resources/org/springframework/batch/samples/metrics/spring-batch-dashboard.json`.

Finally, run the `org.springframework.batch.samples.metrics.BatchMetricsApplication`
class without any argument to start the sample.

### MongoDB sample

This sample is a showcase of MongoDB support in Spring Batch. It copies data from
an input collection to an output collection using `MongoPagingItemReader` and `MongoItemWriter`.

To run the sample, you need to have a MongoDB server up and running on `localhost:27017` 
(you can change these defaults in `mongodb-sample.properties`). If you use docker,
you can run the following command to start a MongoDB server:

```
$>docker run --name mongodb --rm -d -p 27017:27017 mongo
```

Once MongoDB is up and running, run the `org.springframework.batch.samples.mongodb.MongoDBSampleApp`
class without any argument to start the sample.

### Remote step sample

This sample shows how to configure and run a remote step using Spring Integration. The sample consists of a manager application
that launches a job with a remote step, and a worker application that executes the remote step.

First, you need to start the shared job repository database:

```
$>cd spring-batch-samples/src/main/resources/org/springframework/batch/samples/remotestep
$>docker-compose up -d
```

Then, you need to start the worker application. You can do this by running the `org.springframework.batch.samples.remotestep.WorkerConfiguration` class without any argument.

Once the worker is up and running, you can start the manager application by running the `org.springframework.batch.samples.remotestep.ManagerConfiguration` class without any argument.

You should see the manager application waiting for the worker step to finish, and the worker application processing the remote step.

Once the remote step is finished, the manager application will complete the job.

You can stop the docker container running the database by executing:

```
$>docker-compose down
```
### PetClinic sample

This sample uses the [PetClinic Spring application](https://github.com/spring-projects/spring-petclinic) to show how to use
Spring Batch to export data from a relational database table to a flat file.

The job in this sample is a single-step job that exports data from the `owners` table
to a flat file named `owners.csv`.

[PetClinic Sample](src/main/java/org/springframework/batch/samples/petclinic/README.md)

### Adhoc Loop and JMX Sample

This job is simply an infinite loop. It runs forever so it is
useful for testing features to do with stopping and starting jobs.
It is used, for instance, as one of the jobs that can be run from JMX.

[Adhoc Loop and JMX Sample](src/main/java/org/springframework/batch/samples/misc/jmx/README.md)
