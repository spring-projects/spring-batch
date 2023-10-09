## Adhoc Loop and JMX Sample

### About

This job is simply an infinite loop.  It runs forever so it is
useful for testing features to do with stopping and starting jobs.
It is used, for instance, as one of the jobs that can be run from JMX.

The JMX launcher uses an additional XML configuration file
(`adhoc-job-launcher-context.xml`) to set up a `JobOperator` for
running jobs asynchronously (i.e. in a background thread).

The rest of the configuration for this demo consists of exposing
some components from the application context as JMX managed beans.
The `JobOperator` is exposed so that it can be controlled from a
remote client (such as JConsole from the JDK) which does not have
Spring Batch on the classpath. See the Spring Core Reference Guide
for more details on how to customise the JMX configuration.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=RemoteLauncherTests#testPauseJob test
```