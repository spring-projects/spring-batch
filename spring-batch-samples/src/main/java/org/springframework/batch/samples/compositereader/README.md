## Composite ItemReader Sample

### About

This sample shows how to use a composite item reader to read data with
the same format from different data sources.

In this sample, data items of type `Person` are read from two flat files
and a relational database table.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=CompositeItemReaderSampleFunctionalTests#testJobLaunch test
```