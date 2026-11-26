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

import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
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
 * should only be created when the flow reaches them.
 *
 * @author Taehun Lee
 */
class FlowJobIntegrationTests {

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

}
