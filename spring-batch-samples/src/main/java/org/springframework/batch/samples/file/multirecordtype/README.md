### MultiRecord type Input Job

## About

The goal of this sample is to show how to use the `PatternMatchingCompositeLineMapper` API
to process files containing lines of different types:

```
CUST42001customer100012000
CUST42002customer200022000
CUST42003customer300032000
TRADUK21341EAH45978 98.34customer1
TRADUK21341EAH46112 18.12customer2
CUST42004customer400042000
CUST42005customer500052000
TRADUK21341EAH47245 12.78customer3
TRADUK21341EAH48108109.25customer4
TRADUK21341EAH49854123.39customer5
CUST42006customer600062000
TRADUK21341EAH50234 32.45customer6
...
```

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=MultiRecordTypeFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=MultiRecordTypeFunctionalTests#testLaunchJobWithJavaConfig test
```

