## Reader Writer adapter Sample

### About

This sample shows the delegate pattern again, and also the
`ItemReaderAdapter` which is used to adapt a POJO to the
`ItemReader` interface.

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=DelegatingJobFunctionalTests#testLaunchJob test
```