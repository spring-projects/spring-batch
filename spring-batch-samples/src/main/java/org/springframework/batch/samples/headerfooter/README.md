## Header Footer Sample

### About

This sample shows the use of callbacks and listeners to deal with
headers and footers in flat files.  It uses two custom callbacks:

* `HeaderCopyCallback`: copies the header of a file from the
  input to the output.
* `SummaryFooterCallback`: creates a summary footer at the end
  of the output file.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=HeaderFooterSampleFunctionalTests#testJob test
```