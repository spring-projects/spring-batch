## BeanWrapperMapper Sample

### About

This sample shows the use of automatic mapping from fields in a file
to a domain object.  The `Trade` and `Person` objects needed
by the job are created from the Spring configuration using prototype
beans, and then their properties are set using the
`BeanWrapperFieldSetMapper`, which sets properties of the
prototype according to the field names in the file.

Nested property paths are resolved in the same way as normal Spring
binding occurs, but with a little extra leeway in terms of spelling
and capitalisation.  Thus for instance, the `Trade` object has a
property called `customer` (lower case), but the file has been
configured to have a column name `CUSTOMER` (upper case), and
the mapper will accept the values happily.  Underscores instead of
camel-casing (e.g. `CREDIT_CARD` instead of `creditCard`)
also work.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=BeanWrapperMapperSampleJobFunctionalTests#testJobLaunch test
```