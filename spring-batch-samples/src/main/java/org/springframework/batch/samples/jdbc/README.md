### Jdbc Readers and Batch Update sample

## About

The purpose of this sample is to show to usage of the
`JdbcCursorItemReader`/`JdbcPagingItemReader` and the `JdbcBatchItemWriter` to make
efficient updates to a database table.

The `JdbcBatchItemWriter` accepts a special form of
`PreparedStatementSetter` as a (mandatory) dependency.  This is
responsible for copying fields from the item to be written to a
`PreparedStatement` matching the SQL query that has been
injected.  The implementation of the
`CustomerCreditUpdatePreparedStatementSetter` shows best
practice of keeping all the information needed for the execution in
one place, since it contains a static constant value (`QUERY`)
which is used to configure the query for the writer.

## Run the sample

You can run the cursor reader sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=JdbcCursorFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=JdbcCursorFunctionalTests#testLaunchJobWithJavaConfig test
```

You can run the paging reader sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=JdbcPagingFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=JdbcPagingFunctionalTests#testLaunchJobWithJavaConfig test
```

