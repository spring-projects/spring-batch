## Composite ItemWriter Sample

### About

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

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=CompositeItemWriterSampleFunctionalTests#testJobLaunch test
```