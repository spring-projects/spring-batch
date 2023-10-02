### MultiLine Input Job

## About

The goal of this sample is to show how to process input files where a single logical
item spans multiple physical line:

```
BEGIN
INFO,UK21341EAH45,customer1
AMNT,978,98.34
END
BEGIN
INFO,UK21341EAH46,customer2
AMNT,112,18.12
END
...
```

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=MultiLineFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=MultiLineFunctionalTests#testLaunchJobWithJavaConfig test
```

