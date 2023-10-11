### XML Input Output

## About

The goal here is to show the use of XML input and output through
streaming and Spring OXM marshallers and unmarshallers.

The job has a single step that copies `CustomerCredit` data from one XML
file to another:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<customers>
	<customer>
		<name>customer1</name>
		<credit>10</credit>
	</customer>
	<customer>
		<name>customer2</name>
		<credit>20</credit>
	</customer>
	<customer>
		<name>customer3</name>
		<credit>30</credit>
	</customer>
	<customer>
		<name>customer4</name>
		<credit>40</credit>
	</customer>
	<customer>
		<name>customer5</name>
		<credit>50</credit>
	</customer>
</customers>
```


It uses XStream for the object XML conversion,
because this is simple to configure for basic use cases like this
one.  See [Spring OXM documentation](https://docs.spring.io/spring-framework/reference/data-access/oxm.html) for details of other options.

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=XmlFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=XmlFunctionalTests#testLaunchJobWithJavaConfig test
```