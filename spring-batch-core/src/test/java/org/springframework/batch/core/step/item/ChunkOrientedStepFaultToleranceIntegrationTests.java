/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.FatalStepExecutionException;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingExceptionHierarchySkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * Integration tests for the fault-tolerance features of {@link ChunkOrientedStep}.
 *
 * @author Mahmoud Ben Hassine
 */
public class ChunkOrientedStepFaultToleranceIntegrationTests {

	// TODO use parameterized tests for serial and concurrent steps
	// The outcome should be the same for both

	@Test
	void testFaultTolerantChunkOrientedStepSuccess() throws Exception {
		// given
		System.setProperty("skipLimit", "3");
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				FaultTolerantChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons-bad-data.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Assertions.assertEquals(4, stepExecution.getReadCount());
		Assertions.assertEquals(3, stepExecution.getWriteCount());
		Assertions.assertEquals(3, stepExecution.getCommitCount());
		Assertions.assertEquals(2, stepExecution.getRollbackCount());
		Assertions.assertEquals(2, stepExecution.getReadSkipCount());
		Assertions.assertEquals(1, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(3, stepExecution.getSkipCount());
		Assertions.assertEquals(3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
		System.clearProperty("skipLimit");
	}

	@Test
	void testConcurrentFaultTolerantChunkOrientedStepSuccess() throws Exception {
		// given
		System.setProperty("skipLimit", "3");
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentFaultTolerantChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons-bad-data.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Assertions.assertEquals(4, stepExecution.getReadCount());
		Assertions.assertEquals(3, stepExecution.getWriteCount());
		Assertions.assertEquals(3, stepExecution.getCommitCount());
		Assertions.assertEquals(2, stepExecution.getRollbackCount());
		Assertions.assertEquals(2, stepExecution.getReadSkipCount());
		Assertions.assertEquals(1, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(3, stepExecution.getSkipCount());
		Assertions.assertEquals(3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
		System.clearProperty("skipLimit");
	}

	@Test
	void testFaultTolerantChunkOrientedStepFailure() throws Exception {
		// given
		System.setProperty("skipLimit", "1");
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				FaultTolerantChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons-bad-data.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Throwable failureException = stepExecution.getFailureExceptions().iterator().next();
		Assertions.assertInstanceOf(FatalStepExecutionException.class, failureException);
		Assertions.assertInstanceOf(SkipLimitExceededException.class, failureException.getCause());
		Assertions.assertEquals(3, stepExecution.getReadCount());
		Assertions.assertEquals(2, stepExecution.getWriteCount());
		Assertions.assertEquals(1, stepExecution.getCommitCount());
		Assertions.assertEquals(2, stepExecution.getRollbackCount());
		Assertions.assertEquals(1, stepExecution.getReadSkipCount());
		Assertions.assertEquals(0, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(1, stepExecution.getSkipCount());
		Assertions.assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
		System.clearProperty("skipLimit");
	}

	@Test
	void testConcurrentFaultTolerantChunkOrientedStepFailure() throws Exception {
		// given
		System.setProperty("skipLimit", "1");
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentFaultTolerantChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons-bad-data.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Throwable failureException = stepExecution.getFailureExceptions().iterator().next();
		Assertions.assertInstanceOf(FatalStepExecutionException.class, failureException);
		Assertions.assertInstanceOf(SkipLimitExceededException.class, failureException.getCause());
		Assertions.assertEquals(3, stepExecution.getReadCount());
		Assertions.assertEquals(2, stepExecution.getWriteCount());
		Assertions.assertEquals(1, stepExecution.getCommitCount());
		Assertions.assertEquals(2, stepExecution.getRollbackCount());
		Assertions.assertEquals(1, stepExecution.getReadSkipCount());
		Assertions.assertEquals(0, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(1, stepExecution.getSkipCount());
		Assertions.assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
		System.clearProperty("skipLimit");
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5084
	@Test
	void testSkipInReadInSequentialMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				StepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(8, stepExecution.getReadCount());
		Assertions.assertEquals(8, stepExecution.getWriteCount());
		Assertions.assertEquals(2, stepExecution.getCommitCount());
		Assertions.assertEquals(0, stepExecution.getRollbackCount());
		Assertions.assertEquals(2, stepExecution.getReadSkipCount());
		Assertions.assertEquals(0, stepExecution.getWriteSkipCount());
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5084
	@Test
	void testSkipInReadInConcurrentMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(8, stepExecution.getReadCount());
		Assertions.assertEquals(8, stepExecution.getWriteCount());
		Assertions.assertEquals(2, stepExecution.getCommitCount());
		Assertions.assertEquals(0, stepExecution.getRollbackCount());
		Assertions.assertEquals(2, stepExecution.getReadSkipCount());
		Assertions.assertEquals(0, stepExecution.getWriteSkipCount());
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5091
	@Test
	void testExhaustedRetryWithNonSkippableException() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ExhaustedRetryStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Throwable failureException = stepExecution.getFailureExceptions().iterator().next();
		Assertions.assertInstanceOf(FatalStepExecutionException.class, failureException);
		Assertions.assertInstanceOf(RetryException.class, failureException.getCause());
		// the test has a single chunk (3 items with chunk size = 3) that is rolled back
		// upon retry exhaustion
		Assertions.assertEquals(3, stepExecution.getReadCount());
		Assertions.assertEquals(0, stepExecution.getWriteCount());
		Assertions.assertEquals(0, stepExecution.getCommitCount());
		Assertions.assertEquals(1, stepExecution.getRollbackCount());
		Assertions.assertEquals(0, stepExecution.getReadSkipCount());
		// no scan is triggered as the exception is non skippable
		Assertions.assertEquals(0, stepExecution.getWriteSkipCount());
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5210
	@Test
	void testNoDuplicateWritesAfterScanModeInSequentialMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				DuplicateWriteDetectionStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(3, stepExecution.getReadCount());
		Assertions.assertEquals(1, stepExecution.getWriteCount());
		Assertions.assertEquals(2, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5210
	@Test
	void testNoDuplicateWritesAfterScanModeInConcurrentMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentDuplicateWriteDetectionStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(3, stepExecution.getReadCount());
		Assertions.assertEquals(1, stepExecution.getWriteCount());
		Assertions.assertEquals(2, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	@Configuration
	static class StepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			List<String> items = List.of("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 5).reader(new ListItemReader<>(items) {
				@Override
				public @Nullable String read() {
					String item = super.read();
					if ("three".equals(item) || "seven".equals(item)) {
						throw new RuntimeException("Simulated read error on item: " + item);
					}
					return item;
				}
			}).writer(chunk -> {
			})
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	@Configuration
	static class ExhaustedRetryStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			List<String> items = List.of("one", "two", "three");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(chunk -> {
					for (String item : chunk) {
						if (item.equalsIgnoreCase("two")) {
							throw new RuntimeException("Simulated write error on item two");
						}
					}
				})
				.transactionManager(transactionManager)
				.faultTolerant()
				.build();
		}

	}

	@Configuration
	static class ConcurrentStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			List<String> items = List.of("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 5).reader(new ListItemReader<>(items) {
				@Override
				public @Nullable String read() {
					String item = super.read();
					if ("three".equals(item) || "seven".equals(item)) {
						throw new RuntimeException("Simulated read error on item: " + item);
					}
					return item;
				}
			}).writer(chunk -> {
			})
				.transactionManager(transactionManager)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	@Configuration
	static class FaultTolerantChunkOrientedStepConfiguration {

		@Bean
		public Step faultTolerantChunkOrientedStep(JobRepository jobRepository,
				JdbcTransactionManager transactionManager, ItemReader<Person> itemReader,
				ItemProcessor<Person, Person> itemProcessor, ItemWriter<Person> itemWriter) {
			// retry policy configuration
			int retryLimit = 3;
			Set<Class<? extends Throwable>> nonRetryableExceptions = Set.of(FlatFileParseException.class,
					DataIntegrityViolationException.class);
			RetryPolicy retryPolicy = RetryPolicy.builder()
				.maxRetries(retryLimit)
				.excludes(nonRetryableExceptions)
				.build();

			// skip policy configuration
			int skipLimit = Integer.parseInt(System.getProperty("skipLimit"));
			Set<Class<? extends Throwable>> skippableExceptions = Set.of(FlatFileParseException.class,
					DataIntegrityViolationException.class);
			LimitCheckingExceptionHierarchySkipPolicy skipPolicy = new LimitCheckingExceptionHierarchySkipPolicy(
					skippableExceptions, skipLimit);

			return new ChunkOrientedStepBuilder<Person, Person>(jobRepository, 2).reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.transactionManager(transactionManager)
				.faultTolerant()
				.retryPolicy(retryPolicy)
				.skipPolicy(skipPolicy)
				.build();
		}

	}

	@Configuration
	static class ConcurrentFaultTolerantChunkOrientedStepConfiguration {

		@Bean
		public Step concurrentFaulTolerantChunkOrientedStep(JobRepository jobRepository,
				JdbcTransactionManager transactionManager, ItemReader<Person> itemReader,
				ItemProcessor<Person, Person> itemProcessor, ItemWriter<Person> itemWriter) {
			// retry policy configuration
			int retryLimit = 3;
			Set<Class<? extends Throwable>> nonRetrybaleExceptions = Set.of(FlatFileParseException.class,
					DataIntegrityViolationException.class);
			RetryPolicy retryPolicy = RetryPolicy.builder()
				.maxRetries(retryLimit)
				.excludes(nonRetrybaleExceptions)
				.build();

			// skip policy configuration
			int skipLimit = Integer.parseInt(System.getProperty("skipLimit"));
			Set<Class<? extends Throwable>> skippableExceptions = Set.of(FlatFileParseException.class,
					DataIntegrityViolationException.class);
			LimitCheckingExceptionHierarchySkipPolicy skipPolicy = new LimitCheckingExceptionHierarchySkipPolicy(
					skippableExceptions, skipLimit);

			return new ChunkOrientedStepBuilder<Person, Person>(jobRepository, 2).reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.transactionManager(transactionManager)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.retryPolicy(retryPolicy)
				.skipPolicy(skipPolicy)
				.build();
		}

	}

	@Configuration
	static class DuplicateWriteDetectionStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(chunk -> {
					for (String item : chunk) {
						if ("2".equals(item) || "3".equals(item)) {
							throw new RuntimeException("Simulated write error for item: " + item);
						}
						jdbcTemplate.update("INSERT INTO delivery (item_number) VALUES (?)", item);
					}
				})
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	@Configuration
	static class ConcurrentDuplicateWriteDetectionStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(chunk -> {
					for (String item : chunk) {
						if ("2".equals(item) || "3".equals(item)) {
							throw new RuntimeException("Simulated write error for item: " + item);
						}
						jdbcTemplate.update("INSERT INTO delivery (item_number) VALUES (?)", item);
					}
				})
				.transactionManager(transactionManager)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

}