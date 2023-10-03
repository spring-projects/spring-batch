### JPA Readers and Writers sample

## About

The purpose of this sample is to show to usage of the JPA item readers and writers
to read and write data from/to a database with JPA and Hibernate.

## Run the samples

You can run the sample of the `JpaPagingItemReader`/`JpaItemWriter` from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=JpaFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=JpaFunctionalTests#testLaunchJobWithJavaConfig test
```

You can run the sample of the `RepositoryItemReader`/`RepositoryItemWriter` from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=RepositoryFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=RepositoryFunctionalTests#testLaunchJobWithJavaConfig test
```
