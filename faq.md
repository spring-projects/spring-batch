---
layout: documentation_page
---
# Frequently Asked Questions

1. [What's the current release and what are the plans for future releases?](#release)
2. [Is it possible to execute jobs in multiple threads or multiple processes?](#threading)
3. [How can I make an item reader thread safe?](#threading-reader)
4. [What is the Spring Batch philosophy on the use of flexible strategies and default implementations? Can you add a public getter for this or that property?](#flexible)
5. [How does Spring Batch differ from Quartz? Is there a place for them both in a solution?](#quartz)
6. [How do I schedule a job with Spring Batch?](#schedulers)
7. [How does Spring Batch allow project to optimize for performance and scalability (through parallel processing or other)?](#parallel)
8. [How can messaging be used to scale batch architectures?](#messaging)
9. [How can I contribute to Spring Batch?](#contributions)

## <a name="release">&nbsp;</a>What's the current release and what are the plans for future releases?

You can track the progress and planning in JIRA [http://jira.spring.io/browse/BATCH](http://jira.spring.io/browse/BATCH).

## <a name="threading">&nbsp;</a>Is it possible to execute jobs in multiple threads or multiple processes?

There are three ways to approach this - but we recommend exercising caution in the analysis of such requirements (is it really necessary?).

* Add a `TaskExecutor` to the repeatTemplate used to control step execution (the outer step operations). The FactoryBeans provided for configuring Steps (e.g. `FaultTolerantStepFactoryBean`) have a "taskExecutor" property you can set. This works as long as the step is intrinsically restartable (idempotent effectively). The parallel job sample shows how it might work in practice - this uses a "process indicator" pattern to mark input records as complete, inside the business transaction.
* Use the `PartitionStep` to split your step execution explicitly amongst several Step instances. Spring Batch has a local multi-threaded implementation of the main strategy for this (`PartitionHandler`), which makes it a great choice for IO intensive jobs. Remember to use scope="step" for the stateful components in a step executing in this fashion, so that separate instances are created per step execution, and there is no cross talk between threads. See below for more details.
* Use the Remote Chunking approach as implemented in the [spring-batch-integration](https://github.com/spring-projects/spring-batch/tree/master/spring-batch-integration) subproject. This requires some durable middleware (e.g. JMS) for reliable communication between the driving step and the remote workers. The basic idea is to use a special `ItemWriter` on the driving process, and a listener pattern on the worker processes (via a `ChunkProcessor`). See below for more details.

## <a name="threading-reader">&nbsp;</a>How can I make an item reader thread safe

You can synchronize the `read()` method (e.g. by wrapping it in a delegator that does the synchronization).  Remember that you will lose restartability, so best practice is to mark the step as not restartable and to be safe (and efficient) you can also set saveState=false on the reader.

## <a name="flexible">&nbsp;</a>What is the Spring Batch philosophy on the use of flexible strategies and default implementations? Can you add a public getter for this or that property?

There are a great many extension points in Spring Batch for the framework developer (as opposed to the implementor of business logic). We expect clients to create their own more specific strategies that can be plugged in to control things like commit intervals (`CompletionPolicy`), rules about how to deal with exceptions (`ExceptionHandler`), and many others.

In general we try to dissuade users from extending framework classes. The Java language doesn't give us as much flexibility to mark classes and interfaces as internal. Generally you can expect anything at the top level of the source tree in packages 'org.springframework.batch.*' to be public, but not necessarily sub-classable. Extending our concrete implementations of most strategies is discouraged in favour of a composition or forking approach. If your code can use only the interfaces from Spring Batch, that gives you the greatest possible portability.

## <a name="quartz">&nbsp;</a>How does Spring Batch differ from Quartz? Is there a place for them both in a solution?

Spring Batch and Quartz have different goals. Spring Batch provides functionality for processing large volumes of data and Quartz provides functionality for scheduling tasks. So Quartz could complement Spring Batch, but are not excluding technologies. A common combination would be to use Quartz as a trigger for a Spring Batch job using a Cron expression and the Spring Core convenience `SchedulerFactoryBean`.

## <a name="schedulers">&nbsp;</a>How do I schedule a job with Spring Batch?

Use a scheduling tool. There are plenty of them out there. Examples: Quartz, Control-M, Autosys. Quartz doesn't have all the features of Control-M or Autosys - it is supposed to be lightweight. If you want something even more lightweight you can just use the OS (cron, at, etc.).

Simple sequential dependencies can be implemented using the job-steps model of Spring Batch, and the non-sequential features in Spring Batch 2.0. We think this is quite common. And in fact it makes it easier to correct a common mis-use of scehdulers - having hundreds of jobs configured, many of which are not independent, but only depend on one other.

## <a name="parallel">&nbsp;</a>How does Spring Batch allow project to optimize for performance and scalability (through parallel processing or other)?

We see this as one of the roles of the Job or Step. A specific implementation of the `Step` deals with the concern of breaking apart the business logic and sharing it efficiently between parallel processes or processors (see `PartitionStep`). There are a number of technologies that could play a role here. The essence is just a set of concurrent remote calls to distributed agents that can handle some business processing. Since the business processing is already typically modularised - e.g. input an item, process it - Spring Batch can strategise the distribution in a number of ways. One implementation that we have had some experience with is a set of remote web services handling the business processing. We send a specific range of primary keys for the inputs to each of a number of remote calls. The same basic strategy would work with any of the Spring Remoting protocols (plain RMI, HttpInvoker, JMS, Hessian etc.) with little more than a couple of lines change in the execution layer configuration.

## <a name="messaging">&nbsp;</a>How can messaging be used to scale batch architectures?

There is a good deal of practical evidence from existing projects that a pipeline approach to batch processing is highly beneficial, leading to resilience and high throughput. We are often faced with mission-critical applications where audit trails are essential, and guaranteed processing is demanded, but where there are extremely tight limits on performance under load, or where high throughput gives a competitive advantage. Matt Welsh's work shows that a Staged Event Driven Architecture (SEDA) has enormous benefits over more rigid processing architectures, and message-oriented middleware (JMS, AQ, MQ, Tibco etc.) gives us a lot of resilience out of the box. There are particular benefits in a system where there is feedback between downstream and upstream stages, so the number of consumers can be adjusted to account for the amount of demand. So how does this fit into Spring Batch? The [spring-batch-integration](https://github.com/spring-projects/spring-batch/tree/master/spring-batch-integration) module has this pattern implemented in [Spring Integration](http://projects.spring.io/spring-integration/), and can be used to scale up the remote processing of any step with many items to process. See in particular the "chunk" package, and the `ItemWriter` and `ChunkHandler` implementations in there.

## <a name="contributions">&nbsp;</a>How can I contribute to Spring Batch?

Use the community forum to get involved in discussions about the product and its design. There is a process for contributions and eventually becoming a committer. The process is pretty standard for all Apache-licensed projects. You make contributions through [JIRA](http://jira.spring.io/browse/BATCH) (so sign up now); you assign the copyright of any contributions using a standard Apache-like CLA (see the Apache one for example - ours might be slightly different); when the contributions reach a certain level, or you convince us otherwise that you are going to be committed long term, even if part time, then you can become a committer.
