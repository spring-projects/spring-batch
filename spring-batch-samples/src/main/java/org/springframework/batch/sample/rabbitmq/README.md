# AMQP sample Job

## About

This sample shows the use of Spring Batch to write to an `AmqpItemWriter`.
The `AmqpItemReader` and Writer were contributed by Chris Schaefer.
It is modeled after the `JmsItemReader` / Writer implementations, which
are popular models for remote chunking. It leverages the `AmqpTemplate`.

## Run the sample

This example requires the env to have a copy of rabbitmq installed
and running.  The standard dashboard can be used to see the traffic
from the `MessageProducer` to the `AmqpItemWriter`.  Make sure you
launch the `MessageProducer` before launching the test.

You can run the sample from the command line as following:

```
cd spring-batch-samples
# Launch the test using the XML configuration
../mvnw -Dtest=AMQPJobFunctionalTests#testLaunchJobWithXmlConfig test
# Launch the test using the Java configuration
../mvnw -Dtest=AMQPJobFunctionalTests#testLaunchJobWithJavaConfig test