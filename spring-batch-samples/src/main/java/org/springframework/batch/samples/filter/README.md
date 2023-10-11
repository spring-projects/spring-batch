## Customer Filter Sample

### About

This shows the use of the `ItemProcessor` to filter out items by
returning null.  When an item is filtered it leads to an increment
in the `filterCount` in the step execution.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=CustomerFilterJobFunctionalTests#testFilterJob test
```