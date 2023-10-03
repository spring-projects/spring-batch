### JPA Reader and Writer sample

## About

The purpose of this sample is to show to usage of the `JpaPagingItemReader`
and the `JpaItemWriter` to read and write data from/to a database with JPA.

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=JpaFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=JpaFunctionalTests#testLaunchJobWithJavaConfig test
```
