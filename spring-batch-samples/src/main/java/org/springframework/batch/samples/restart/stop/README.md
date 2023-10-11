## Stop / Restart Sample

### About

This sample has a single step that is an infinite loop, reading and
writing fake data.  It is used to demonstrate stop signals and
restart capabilities.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=JobOperatorFunctionalTests#testStartStopResumeJob test
```