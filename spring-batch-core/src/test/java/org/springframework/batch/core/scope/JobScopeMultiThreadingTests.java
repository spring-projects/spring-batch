/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.scope;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemWriter;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a {@link JobScope job-scoped} bean can be used from the threads that take
 * part in a job execution, and that all of them see the same instance.
 *
 * @author Mahmoud Ben Hassine
 */
class JobScopeMultiThreadingTests {

	/**
	 * Identities of the job-scoped bean instances seen by the job's components. Static
	 * because the components are lambdas that capture the job-scoped proxy.
	 */
	private static final Set<String> seenInstances = ConcurrentHashMap.newKeySet();

	@BeforeEach
	void setUp() {
		seenInstances.clear();
	}

	@Test
	void testJobScopedBeanInSequentialSteps() throws Exception {
		JobExecution jobExecution = runJob("sequentialStepsJob");

		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode(),
				failures(jobExecution));
		assertEquals(1, seenInstances.size());
	}

	@Test
	void testJobScopedBeanInMultiThreadedStep() throws Exception {
		JobExecution jobExecution = runJob("multiThreadedStepJob");

		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode(),
				failures(jobExecution));
		assertEquals(1, seenInstances.size());
	}

	@Test
	void testJobScopedBeanInParallelFlows() throws Exception {
		JobExecution jobExecution = runJob("parallelFlowsJob");

		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode(),
				failures(jobExecution));
		assertEquals(1, seenInstances.size());
	}

	@Test
	void testJobScopedBeanInPartitionedStep() throws Exception {
		JobExecution jobExecution = runJob("partitionedStepJob");

		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode(),
				failures(jobExecution));
		assertEquals(1, seenInstances.size());
	}

	private JobExecution runJob(String jobName) throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfiguration.class)) {
			Job job = context.getBean(jobName, Job.class);
			return context.getBean(JobOperator.class).start(job, new JobParameters());
		}
	}

	private static String failures(JobExecution jobExecution) {
		return "Job failed with: " + jobExecution.getAllFailureExceptions() + ", step exit statuses: "
				+ jobExecution.getStepExecutions().stream().map(StepExecution::getExitStatus).toList();
	}

	public static class JobScopedBean {

		private final String id = Integer.toHexString(System.identityHashCode(this));

		// accessed through a getter as the bean is injected as a scoped proxy
		public String getId() {
			return this.id;
		}

	}

	@Configuration
	static class JobConfiguration extends JdbcDefaultBatchConfiguration {

		@Bean
		@JobScope
		JobScopedBean jobScopedBean() {
			return new JobScopedBean();
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		Job sequentialStepsJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				JobScopedBean jobScopedBean) {
			return new JobBuilder("sequentialStepsJob", jobRepository)
				.start(taskletStep("step1", jobRepository, transactionManager, jobScopedBean))
				.next(taskletStep("step2", jobRepository, transactionManager, jobScopedBean))
				.build();
		}

		@Bean
		Job multiThreadedStepJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				JobScopedBean jobScopedBean) {
			Step step = new ChunkOrientedStepBuilder<Integer, Integer>(jobRepository, 1)
				.reader(new ListItemReader<>(List.of(1, 2, 3, 4)))
				.processor(item -> {
					seenInstances.add(jobScopedBean.getId());
					return item;
				})
				.writer(new ListItemWriter<>())
				.transactionManager(transactionManager)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
			return new JobBuilder("multiThreadedStepJob", jobRepository).start(step).build();
		}

		@Bean
		Job parallelFlowsJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				JobScopedBean jobScopedBean) {
			Flow flow1 = new FlowBuilder<Flow>("flow1")
				.start(taskletStep("step1", jobRepository, transactionManager, jobScopedBean))
				.build();
			Flow flow2 = new FlowBuilder<Flow>("flow2")
				.start(taskletStep("step2", jobRepository, transactionManager, jobScopedBean))
				.build();
			Flow parallelFlows = new FlowBuilder<Flow>("parallelFlows").split(new SimpleAsyncTaskExecutor())
				.add(flow1, flow2)
				.build();
			return new JobBuilder("parallelFlowsJob", jobRepository).start(parallelFlows).end().build();
		}

		@Bean
		Job partitionedStepJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				JobScopedBean jobScopedBean) {
			Step workerStep = taskletStep("workerStep", jobRepository, transactionManager, jobScopedBean);
			Partitioner partitioner = gridSize -> Map.of("partition0", new ExecutionContext(), "partition1",
					new ExecutionContext());
			Step step = new StepBuilder("partitionedStep", jobRepository).partitioner("workerStep", partitioner)
				.step(workerStep)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.gridSize(2)
				.build();
			return new JobBuilder("partitionedStepJob", jobRepository).start(step).build();
		}

		private Step taskletStep(String name, JobRepository jobRepository,
				PlatformTransactionManager transactionManager, JobScopedBean jobScopedBean) {
			return new StepBuilder(name, jobRepository).tasklet((contribution, chunkContext) -> {
				seenInstances.add(jobScopedBean.getId());
				return RepeatStatus.FINISHED;
			}, transactionManager).build();
		}

	}

}
