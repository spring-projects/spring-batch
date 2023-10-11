### Fixed Length File Import Job

## About

The goal is to demonstrate a typical scenario of reading data
from a fixed length file, processing it and writing it to another file.

In this example we are using a simple fixed length record structure
that can be found in the project at
`src/main/resources/org/springframework/batch/samples/file/fixed/data`.
The fixed length records look like this:

```
customer110
customer220
customer330
customer440
customer550
customer660
```

Looking back to the configuration of the reader you will this is
configured in the fixed column ranges:

FieldName | Range
--------- | :----:
name      |   1,9  
credit    |   10,11

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=FixedLengthFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=FixedLengthFunctionalTests#testLaunchJobWithJavaConfig test
```

