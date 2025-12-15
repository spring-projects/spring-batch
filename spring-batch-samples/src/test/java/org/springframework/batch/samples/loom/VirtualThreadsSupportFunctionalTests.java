/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.samples.loom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * This test suite is about identifying the places where a {@link TaskExecutor} is used in
 * Spring Batch and testing the usage of virtual threads through the
 * {@link VirtualThreadTaskExecutor} from Spring Framework.
 * <p>
 * The scope here is only correctness, ie make sure that Spring Batch semantics are still
 * valid with virtual threads as with platform threads. Performance is out-of-scope for
 * now, only correctness is addressed for the time being.
 * <p>
 * Here are the places where a {@link TaskExecutor} is used in production code:
 * <ul>
 * <li>{@link TaskExecutorJobOperator#setTaskExecutor}: to launch jobs in background
 * threads</li>
 * <li>{@link TaskletStepBuilder#taskExecutor(TaskExecutor)}: to execute steps
 * concurrently</li>
 * <li>{@link FlowBuilder#split(TaskExecutor)}: to execute steps in parallel</li>
 * <li>{@link AsyncItemProcessor#setTaskExecutor}: to process items concurrently</li>
 * <li>{@link TaskExecutorPartitionHandler#setTaskExecutor}: to execute workers of a
 * partitioned steps in parallel</li>
 * <li>{@link SystemCommandTasklet#setTaskExecutor}: to run the OS command in a separate
 * thread</li>
 * </ul>
 * Each use case is covered by a test method with its own application context.
 *
 * @author Mahmoud Ben Hassine
 */
@EnabledForJreRange(min = JRE.JAVA_21)
public class VirtualThreadsSupportFunctionalTests {

	@Test
	public void testJobLaunchingWithVirtualThreads() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationForLaunchingJobsWithVirtualThreads.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		JobRepository jobRepository = context.getBean(JobRepository.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		// should wait for virtual threads to finish, otherwise the following assertion
		// might be executed before the virtual thread running the job is finished
		// and therefore will fail.
		while (jobRepository.getJobExecution(jobExecution.getId()).isRunning()) {
			Thread.sleep(100);
		}
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		String message = (String) jobExecution.getExecutionContext().get("message");
		Assertions.assertNotNull(message);
		Assertions.assertTrue(message.contains("VirtualThread["));
		Assertions.assertTrue(message.contains("spring-batch-"));
		Assertions.assertTrue(message.contains("Hello virtual threads world!"));
	}

	@Test
	public void testConcurrentStepsWithVirtualThreads() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationForRunningConcurrentStepsWithVirtualThreads.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(6, stepExecution.getReadCount());
		Assertions.assertEquals(6, stepExecution.getWriteCount());

	}

	@Disabled("This test is flaky on CI")
	@Test
	public void testParallelStepsWithVirtualThreads() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationForRunningParallelStepsWithVirtualThreads.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Test
	public void testAsyncItemProcessingWithVirtualThreads() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationForAsynchronousItemProcessingWithVirtualThreads.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(6, stepExecution.getReadCount());
		Assertions.assertEquals(6, stepExecution.getWriteCount());
	}

	@Test
	public void testLocalPartitioningWithVirtualThreads() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationForRunningPartitionedStepsWithVirtualThreads.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions.assertEquals(5, jobExecution.getStepExecutions().size());
	}

	@Test
	public void testSystemCommandTaskletWithVirtualThreads() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationForRunningSystemCommandTaskletsWithVirtualThreads.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

}
