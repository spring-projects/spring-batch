## Quartz Sample

FIXME: Update job configuration and classes imported from Spring Framework

### About

The goal is to demonstrate how to schedule job execution using
Quartz scheduler.  In this case there is no unit test to launch the
sample because it just re-uses the football job.  There is a main
method in `JobRegistryBackgroundJobRunner` and an Eclipse launch
configuration which runs it with arguments to pick up the football
job.

The additional XML configuration for this job is in
`quartz-job-launcher.xml`, and it also re-uses
`footballJob.xml`

The configuration declares a `JobLauncher` bean. The launcher
bean is different from the other samples only in that it uses an
asynchronous task executor, so that the jobs are launched in a
separate thread to the main method:

```xml
<bean id="jobLauncher" class="org.springframework.batch.core.launch.support.TaskExecutorJobLauncher">
  <property name="jobRepository" ref="jobRepository" />
  <property name="taskExecutor">
    <bean class="org.springframework.core.task.SimpleAsyncTaskExecutor" />
  </property>
</bean>
```

Also, a Quartz `JobDetail` is defined using a Spring
`JobDetailBean` as a convenience.

```xml
<bean id="jobDetail" class="org.springframework.scheduling.quartz.JobDetailBean">
    <property name="jobClass" value="org.springframework.batch.sample.misc.quartz.JobLauncherDetails" />
    <property name="group" value="quartz-batch" />
    <property name="jobDataAsMap">
        <map>
            <entry key="jobName" value="footballJob"/>
            <entry key="jobLocator" value-ref="jobRegistry"/>
            <entry key="jobLauncher" value-ref="jobLauncher"/>
        </map>
    </property>
</bean>
```

Finally, a trigger with a scheduler is defined that will launch the
job detail every 10 seconds:

```xml
<bean class="org.springframework.scheduling.quartz.SchedulerFactoryBean">
  <property name="triggers">
    <bean id="cronTrigger" class="org.springframework.scheduling.quartz.CronTriggerBean">
      <property name="jobDetail" ref="jobDetail" />
      <property name="cronExpression" value="0/10 * * * * ?" />
    </bean>
  </property>
</bean>
```

The job is thus scheduled to run every 10 seconds.  In fact it
should be successful on the first attempt, so the second and
subsequent attempts should through a
`JobInstanceAlreadyCompleteException`.  In a production system,
the job detail would probably be modified to account for this
exception (e.g. catch it and re-submit with a new set of job
parameters).  The point here is that Spring Batch guarantees that
the job execution is idempotent - you can never inadvertently
process the same data twice.

## Run the sample

TODO