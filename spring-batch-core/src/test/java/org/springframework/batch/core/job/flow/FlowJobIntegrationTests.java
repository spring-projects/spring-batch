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
package org.springframework.batch.core.job.flow;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for {@link FlowJob} with {@link JobScope job-scoped} steps, which
 * should only be created when the flow reaches them. Their names are checked for
 * duplicates at that time.
 *
 * @author Taehun Lee
 */
class FlowJobIntegrationTests {

	private CapturingAppender appender;

	private Logger jobLogger;

	private Level previousLevel;

	@BeforeEach
	void setUp() {
		this.appender = new CapturingAppender();
		this.appender.start();
		this.jobLogger = LoggerContext.getContext(false).getLogger(AbstractJob.class.getName());
		this.previousLevel = this.jobLogger.getLevel();
		this.jobLogger.setLevel(Level.WARN);
		this.jobLogger.addAppender(this.appender);
	}

	@AfterEach
	void tearDown() {
		this.jobLogger.removeAppender(this.appender);
		this.jobLogger.setLevel(this.previousLevel);
		this.appender.stop();
	}

	@Test
	void testJobScopedStepIsCreatedWhenTheFlowReachesIt() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				JobScopedStepConfiguration.class)) {
			JobOperator jobOperator = context.getBean(JobOperator.class);
			Job job = context.getBean(Job.class);
			JobExecution jobExecution = jobOperator.start(job, new JobParameters());
			assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
			assertEquals("hello", jobExecution.getExecutionContext().get("consumedValue"));
		}
	}

	@Test
	void testJobScopedStepOnUnreachedBranchIsNotCreated() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				UnreachedJobScopedStepConfiguration.class)) {
			JobOperator jobOperator = context.getBean(JobOperator.class);
			Job job = context.getBean(Job.class);
			JobExecution jobExecution = jobOperator.start(job, new JobParameters());
			assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
			assertEquals(1, jobExecution.getStepExecutions().size());
			assertEquals("mainStep", jobExecution.getStepExecutions().iterator().next().getStepName());
		}
	}

	@Test
	void testJobScopedStepsSharingTheSameNameAreReportedWhenTheFlowReachesThem() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DuplicateJobScopedStepNameConfiguration.class)) {
			JobOperator jobOperator = context.getBean(JobOperator.class);
			Job job = context.getBean(Job.class);
			JobExecution jobExecution = jobOperator.start(job, new JobParameters());
			assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
			assertEquals(2, jobExecution.getStepExecutions().size());
			assertEquals(
					List.of("Step name [jobScopedStep] is used by more than one step in job [job]. "
							+ "Step names should be unique within a job: restart relies on them."),
					this.appender.warnings());
		}
	}

	@Test
	void testJobScopedStepSharingItsNameWithANonScopedStepIsReportedWhenTheFlowReachesIt() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DuplicateJobScopedAndNonScopedStepNameConfiguration.class)) {
			JobOperator jobOperator = context.getBean(JobOperator.class);
			Job job = context.getBean(Job.class);
			JobExecution jobExecution = jobOperator.start(job, new JobParameters());
			assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
			assertEquals(2, jobExecution.getStepExecutions().size());
			assertEquals(
					List.of("Step name [sharedName] is used by more than one step in job [job]. "
							+ "Step names should be unique within a job: restart relies on them."),
					this.appender.warnings());
		}
	}

	@Test
	void testSameJobScopedStepUsedSeveralTimesIsNotReported() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ReusedJobScopedStepConfiguration.class)) {
			JobOperator jobOperator = context.getBean(JobOperator.class);
			Job job = context.getBean(Job.class);
			JobExecution jobExecution = jobOperator.start(job, new JobParameters());
			assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
			assertEquals(2, jobExecution.getStepExecutions().size());
			assertEquals(List.of(), this.appender.warnings());
		}
	}

	@Configuration
	@EnableBatchProcessing
	static class JobScopedStepConfiguration {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		Job job(JobRepository jobRepository, Step producerStep, Step consumerStep) {
			return new JobBuilder("job", jobRepository).flow(producerStep).next(consumerStep).end().build();
		}

		@Bean
		Step producerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("producerStep", jobRepository).tasklet((contribution, chunkContext) -> {
				contribution.getStepExecution().getJobExecution().getExecutionContext().put("value", "hello");
				return RepeatStatus.FINISHED;
			}, transactionManager).build();
		}

		@Bean
		@JobScope
		Step consumerStep(@Value("#{jobExecutionContext['value']}") String value, JobRepository jobRepository,
				PlatformTransactionManager transactionManager) {
			return new StepBuilder("consumerStep", jobRepository).tasklet((contribution, chunkContext) -> {
				contribution.getStepExecution().getJobExecution().getExecutionContext().put("consumedValue", value);
				return RepeatStatus.FINISHED;
			}, transactionManager).build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	static class UnreachedJobScopedStepConfiguration {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		Job job(JobRepository jobRepository, Step mainStep, Step recoveryStep) {
			return new JobBuilder("job", jobRepository).start(mainStep)
				.on(ExitStatus.FAILED.getExitCode())
				.to(recoveryStep)
				.from(mainStep)
				.on("*")
				.end()
				.end()
				.build();
		}

		@Bean
		Step mainStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("mainStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

		// only reached when mainStep fails, after the error has been stored in the job
		// execution context
		@Bean
		@JobScope
		Step recoveryStep(@Value("#{jobExecutionContext['error']}") String error, JobRepository jobRepository,
				PlatformTransactionManager transactionManager) {
			Objects.requireNonNull(error, "error must be set before the recovery step is created");
			return new StepBuilder("recoveryStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	static class DuplicateJobScopedStepNameConfiguration {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		Job job(JobRepository jobRepository, Step firstStep, Step secondStep) {
			return new JobBuilder("job", jobRepository).flow(firstStep).next(secondStep).end().build();
		}

		@Bean
		@JobScope
		Step firstStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("jobScopedStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

		@Bean
		@JobScope
		Step secondStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("jobScopedStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	static class DuplicateJobScopedAndNonScopedStepNameConfiguration {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		Job job(JobRepository jobRepository, Step nonScopedStep, Step jobScopedStep) {
			return new JobBuilder("job", jobRepository).flow(nonScopedStep).next(jobScopedStep).end().build();
		}

		@Bean
		Step nonScopedStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("sharedName", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

		@Bean
		@JobScope
		Step jobScopedStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("sharedName", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	static class ReusedJobScopedStepConfiguration {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		Job job(JobRepository jobRepository, Step jobScopedStep) {
			Flow firstFlow = new FlowBuilder<Flow>("firstFlow").start(jobScopedStep).build();
			Flow secondFlow = new FlowBuilder<Flow>("secondFlow").start(jobScopedStep).build();
			return new JobBuilder("job", jobRepository).start(firstFlow).next(secondFlow).end().build();
		}

		@Bean
		@JobScope
		Step jobScopedStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("jobScopedStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

	}

	/** Collects the events logged by {@link AbstractJob}. */
	static class CapturingAppender extends AbstractAppender {

		private final List<LogEvent> events = new CopyOnWriteArrayList<>();

		CapturingAppender() {
			super("capturing", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			this.events.add(event.toImmutable());
		}

		List<String> warnings() {
			return this.events.stream()
				.filter(event -> event.getLevel() == Level.WARN)
				.map(event -> event.getMessage().getFormattedMessage())
				.toList();
		}

	}

}
