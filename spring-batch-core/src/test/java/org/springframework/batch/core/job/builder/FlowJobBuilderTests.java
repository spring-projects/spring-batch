/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.*;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class FlowJobBuilderTests {

	private JobRepository jobRepository;

	private JobExecution execution;

	private JobInstance jobInstance;

	private final StepSupport step1 = new StepSupport("step1") {
		@Override
		public void execute(StepExecution stepExecution)
				throws JobInterruptedException, UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}
	};

	private final StepSupport fails = new StepSupport("fails") {
		@Override
		public void execute(StepExecution stepExecution)
				throws JobInterruptedException, UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.FAILED);
			stepExecution.setExitStatus(ExitStatus.FAILED);
			jobRepository.update(stepExecution);
		}
	};

	private final StepSupport step2 = new StepSupport("step2") {
		@Override
		public void execute(StepExecution stepExecution)
				throws JobInterruptedException, UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}
	};

	private final StepSupport step3 = new StepSupport("step3") {
		@Override
		public void execute(StepExecution stepExecution)
				throws JobInterruptedException, UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}
	};

	@BeforeEach
	void init() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.generateUniqueName(true)
			.build();
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		JobParameters jobParameters = new JobParameters();
		jobInstance = jobRepository.createJobInstance("flow", jobParameters);
		execution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
	}

	@Test
	void testBuildOnOneLine() throws JobInterruptedException {
		FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1)
			.on("COMPLETED")
			.to(step2)
			.end()
			.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildSingleFlow() throws JobInterruptedException {
		Flow flow = new FlowBuilder<Flow>("subflow").from(step1).next(step2).build();
		FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(flow).end().preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildSingleFlowAddingStepsViaNext() throws JobInterruptedException {
		Flow flow = new FlowBuilder<Flow>("subflow").next(step1).next(step2).build();
		FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(flow).end().preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildOverTwoLines() throws JobInterruptedException {
		FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1).on("COMPLETED").to(step2).end();
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildSubflow() throws JobInterruptedException {
		Flow flow = new FlowBuilder<Flow>("subflow").from(step1).end();
		JobFlowBuilder builder = new JobBuilder("flow", jobRepository).start(flow);
		builder.on("COMPLETED").to(step2);
		builder.end().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	// FIXME work in the IDE but not on the command line
	@Disabled
	@Test
	void testBuildSplit() throws JobInterruptedException {
		Flow flow = new FlowBuilder<Flow>("subflow").from(step1).end();
		SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step2);
		builder.split(new SimpleAsyncTaskExecutor()).add(flow).end();
		builder.preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testNestedSplitsWithSingleThread() throws JobInterruptedException {
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(1);

		FlowBuilder<SimpleFlow> flowBuilder = new FlowBuilder<>("flow");
		FlowBuilder.SplitBuilder<SimpleFlow> splitBuilder = flowBuilder.split(taskExecutor);
		splitBuilder.add(new FlowBuilder<Flow>("subflow1").from(step1).end());
		splitBuilder.add(new FlowBuilder<Flow>("subflow2").from(step2).end());
		Job job = new JobBuilder("job", jobRepository).start(flowBuilder.build()).end().build();
		job.execute(execution);

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	// FIXME work in the IDE but not on the command line
	@Disabled
	@Test
	void testBuildSplitUsingStartAndAdd_BATCH_2346() throws JobInterruptedException {
		Flow subflow1 = new FlowBuilder<Flow>("subflow1").from(step2).end();
		Flow subflow2 = new FlowBuilder<Flow>("subflow2").from(step3).end();
		Flow splitflow = new FlowBuilder<Flow>("splitflow").start(subflow1)
			.split(new SimpleAsyncTaskExecutor())
			.add(subflow2)
			.build();

		FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(splitflow).end();
		builder.preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	// FIXME work in the IDE but not on the command line
	@Disabled
	@Test
	void testBuildSplit_BATCH_2282() throws JobInterruptedException {
		Flow flow1 = new FlowBuilder<Flow>("subflow1").from(step1).end();
		Flow flow2 = new FlowBuilder<Flow>("subflow2").from(step2).end();
		Flow splitFlow = new FlowBuilder<Flow>("splitflow").split(new SimpleAsyncTaskExecutor())
			.add(flow1, flow2)
			.build();
		FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(splitFlow).end();
		builder.preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildDecision() throws JobInterruptedException {
		JobExecutionDecider decider = new JobExecutionDecider() {
			private int count = 0;

			@Override
			public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
				count++;
				return count < 2 ? new FlowExecutionStatus("ONGOING") : FlowExecutionStatus.COMPLETED;
			}
		};
		step1.setAllowStartIfComplete(true);
		SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
		builder.next(decider).on("COMPLETED").end().from(decider).on("*").to(step1).end();
		builder.preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildWithDeciderAtStart() throws JobInterruptedException {
		JobExecutionDecider decider = new JobExecutionDecider() {
			private int count = 0;

			@Override
			public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
				count++;
				return count < 2 ? new FlowExecutionStatus("ONGOING") : FlowExecutionStatus.COMPLETED;
			}
		};
		JobFlowBuilder builder = new JobBuilder("flow", jobRepository).start(decider);
		builder.on("COMPLETED").end().from(decider).on("*").to(step1).end();
		builder.build().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	void testBuildWithDeciderPriorityOnWildcardCount() throws JobInterruptedException {
		JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("COMPLETED_PARTIALLY");
		JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
		builder.on("**").end();
		builder.on("*").fail();
		builder.build().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	void testBuildWithDeciderPriorityWithEqualWildcard() throws JobInterruptedException {
		JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("COMPLETED_PARTIALLY");
		JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
		builder.on("COMPLETED*").end();
		builder.on("*").fail();
		builder.build().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	void testBuildWithDeciderPriority() throws JobInterruptedException {
		JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("COMPLETED_PARTIALLY");
		JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
		builder.on("COMPLETED_PARTIALLY").end();
		builder.on("COMPLETED*").fail();
		builder.build().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	void testBuildWithWildcardDeciderPriority() throws JobInterruptedException {
		JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("COMPLETED_PARTIALLY");
		JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
		builder.on("COMPLETED_?ARTIALLY").end();
		builder.on("COMPLETED_*ARTIALLY").fail();
		builder.build().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	void testBuildWithDeciderPrioritySubstringAndWildcard() throws JobInterruptedException {
		JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("CONTINUABLE");
		JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
		builder.on("CONTINUABLE").end();
		builder.on("CONTIN*").fail();
		builder.build().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	void testBuildWithIntermediateSimpleJob() throws JobInterruptedException {
		SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
		builder.on("COMPLETED").to(step2).end();
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildWithIntermediateSimpleJobTwoSteps() throws JobInterruptedException {
		SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1).next(step2);
		builder.on("FAILED").to(step3).end();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testBuildWithCustomEndState() throws JobInterruptedException {
		SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
		builder.on("COMPLETED").end("FOO");
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals("FOO", execution.getExitStatus().getExitCode());
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	void testBuildWithStop() throws JobInterruptedException {
		SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
		builder.on("COMPLETED").stop();
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		assertEquals("STOPPED", execution.getExitStatus().getExitCode());
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	void testBuildWithStopAndRestart() throws Exception {
		SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(fails);
		builder.on("FAILED").stopAndRestart(step2);
		Job job = builder.build();
		job.execute(execution);
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());

		JobParameters jobParameters = new JobParameters();
		execution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		job.execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());
		assertEquals("step2", execution.getStepExecutions().iterator().next().getStepName());
	}

	@Test
	void testBuildWithJobScopedStep() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder().addLong("chunkSize", 2L).toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@EnableBatchProcessing
	@Configuration
	@EnableJdbcJobRepository
	static class JobConfiguration {

		@Bean
		@JobScope
		public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				@Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(chunkSize, transactionManager)
				.reader(new ListItemReader<>(Arrays.asList(1, 2, 3, 4)))
				.writer(items -> {
				})
				.build();
		}

		@Bean
		public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			Step step = step(jobRepository, transactionManager, null);
			return new JobBuilder("job", jobRepository).flow(step).build().build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

	// FIXME work in the IDE but not on the command line
	@Disabled
	@Test
	public void testBuildSplitWithParallelFlow() throws InterruptedException, JobInterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Step longExecutingStep = new StepBuilder("longExecutingStep", jobRepository).tasklet((stepContribution, b) -> {
			Thread.sleep(500L);
			return RepeatStatus.FINISHED;
		}, new ResourcelessTransactionManager()).build();

		Step interruptedStep = new StepBuilder("interruptedStep", jobRepository).tasklet((stepContribution, b) -> {
			stepContribution.getStepExecution().setTerminateOnly();
			return RepeatStatus.FINISHED;
		}, new ResourcelessTransactionManager()).build();

		Step nonExecutableStep = new StepBuilder("nonExecutableStep", jobRepository).tasklet((stepContribution, b) -> {
			countDownLatch.countDown();
			return RepeatStatus.FINISHED;
		}, new ResourcelessTransactionManager()).build();

		Flow twoStepFlow = new FlowBuilder<SimpleFlow>("twoStepFlow").start(longExecutingStep)
			.next(nonExecutableStep)
			.build();
		Flow interruptedFlow = new FlowBuilder<SimpleFlow>("interruptedFlow").start(interruptedStep).build();

		Flow splitFlow = new FlowBuilder<Flow>("splitFlow").split(new SimpleAsyncTaskExecutor())
			.add(interruptedFlow, twoStepFlow)
			.build();
		FlowJobBuilder jobBuilder = new JobBuilder("job", jobRepository).start(splitFlow).build();
		jobBuilder.preventRestart().build().execute(execution);

		boolean isExecutedNonExecutableStep = countDownLatch.await(1, TimeUnit.SECONDS);
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		Assertions.assertFalse(isExecutedNonExecutableStep);
	}

}
