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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tests for scan mode functionality in {@link ChunkOrientedStep}.
 *
 * @author KMGeon
 * @author MinChul Son
 */
class ChunkOrientedStepScanModeIntegrationTests {

	// Issue https://github.com/spring-projects/spring-batch/issues/5210
	@Test
	void testScanModeExecutesAfterRollbackInSequentialMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				SequentialScanModeStepConfiguration.class);
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
		Assertions.assertEquals(3, stepExecution.getRollbackCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5210
	@Test
	void testScanModeExecutesAfterRollbackInConcurrentMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentScanModeStepConfiguration.class);
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
		Assertions.assertEquals(3, stepExecution.getRollbackCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5210
	@Test
	void testScanModeWithMultipleSuccessfulItemsBeforeFailure() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				MultipleSuccessfulItemsStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(5, stepExecution.getReadCount());
		Assertions.assertEquals(4, stepExecution.getWriteCount());
		Assertions.assertEquals(1, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '2'", Integer.class));
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '4'", Integer.class));
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '5'", Integer.class));
		Assertions.assertEquals(4, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5210
	@Test
	void testScanModeProcessesItemsOneByOne() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				TrackingWriterStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		TrackingWriterStepConfiguration config = context.getBean(TrackingWriterStepConfiguration.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());

		// Verify writer call sequence demonstrates scan mode behavior:
		// 1st call: chunk write with all 3 items [1, 2, 3] -> fails on item "2"
		// After rollback, scan mode processes items one-by-one:
		// 2nd call: [1] -> success
		// 3rd call: [2] -> fails, skipped
		// 4th call: [3] -> fails, skipped
		List<List<String>> writerCalls = config.getWriterCalls();

		Assertions.assertEquals(4, writerCalls.size(), "Writer should be called 4 times (1 chunk + 3 scan)");
		Assertions.assertEquals(List.of("1", "2", "3"), writerCalls.get(0), "First call should have full chunk");
		Assertions.assertEquals(List.of("1"), writerCalls.get(1), "Scan mode: first item individually");
		Assertions.assertEquals(List.of("2"), writerCalls.get(2), "Scan mode: second item individually");
		Assertions.assertEquals(List.of("3"), writerCalls.get(3), "Scan mode: third item individually");
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5210
	@Test
	void testScanModeWhenAllItemsFail() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				AllItemsFailStepConfiguration.class);
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
		Assertions.assertEquals(0, stepExecution.getWriteCount());
		Assertions.assertEquals(3, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	@Configuration
	static class SequentialScanModeStepConfiguration {

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
	static class ConcurrentScanModeStepConfiguration {

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

	@Configuration
	static class MultipleSuccessfulItemsStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3", "4", "5");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 5).reader(new ListItemReader<>(items))
				.writer(chunk -> {
					for (String item : chunk) {
						if ("3".equals(item)) {
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
	static class AllItemsFailStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(chunk -> {
					for (String item : chunk) {
						throw new RuntimeException("Simulated write error for item: " + item);
					}
				})
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5377
	@Test
	void testSkipPolicyWithJpaLikeRollbackOnlyBehaviorInSequentialMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				JpaLikeRollbackOnlySequentialStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		// Without the fix, item "3" would fail with "Transaction silently rolled back
		// because it has been marked as rollback-only" (JPA-like behavior) causing the
		// step to fail with a NonSkippableWriteException. With the fix, each scan item
		// runs in its own transaction so item "3" succeeds.
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(3, stepExecution.getReadCount());
		Assertions.assertEquals(2, stepExecution.getWriteCount());
		Assertions.assertEquals(1, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '3'", Integer.class));
		Assertions.assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5377
	@Test
	void testSkipPolicyWithJpaLikeRollbackOnlyBehaviorInConcurrentMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				JpaLikeRollbackOnlyConcurrentStepConfiguration.class);
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
		Assertions.assertEquals(2, stepExecution.getWriteCount());
		Assertions.assertEquals(1, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '3'", Integer.class));
		Assertions.assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	/**
	 * Simulates JPA-like rollback-only behavior: when a write fails, the transaction is
	 * marked as rollback-only (as JPA/Hibernate does after a flush failure). Subsequent
	 * writes in the same transaction would then throw "Transaction silently rolled back
	 * because it has been marked as rollback-only".
	 */
	private static ItemWriter<String> jpaLikeWriter(JdbcTemplate jdbcTemplate) {
		ThreadLocal<Boolean> rollbackOnly = ThreadLocal.withInitial(() -> false);
		return chunk -> {
			// Simulate JPA behavior: if the current "session" is rollback-only,
			// throw as JPA/Hibernate would before even attempting the write
			if (rollbackOnly.get()) {
				throw new RuntimeException(
						"Transaction silently rolled back because it has been marked as rollback-only");
			}
			for (String item : chunk) {
				if ("2".equals(item)) {
					// Simulate JPA flush failure: mark session rollback-only and register
					// cleanup synchronization (reset after transaction completion)
					rollbackOnly.set(true);
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							rollbackOnly.set(false);
						}
					});
					throw new RuntimeException("Simulated JPA constraint violation for item: " + item);
				}
				jdbcTemplate.update("INSERT INTO delivery (item_number) VALUES (?)", item);
			}
		};
	}

	/**
	 * Simulates JPA-like global rollback-only behavior by marking the underlying
	 * {@link ConnectionHolder} as rollback-only when an item fails. This causes
	 * {@link org.springframework.transaction.support.AbstractPlatformTransactionManager}
	 * to detect a globally-rolled-back transaction during commit and, without the fix in
	 * {@code ChunkOrientedStep.doExecute()}, throw an
	 * {@link org.springframework.transaction.UnexpectedRollbackException}. The fix
	 * explicitly calls {@code transactionStatus.setRollbackOnly()} before returning from
	 * the lambda so that the local rollback-only flag is set and the transaction manager
	 * performs a clean rollback instead.
	 */
	private static ItemWriter<String> globalRollbackOnlyWriter(JdbcTemplate jdbcTemplate) {
		DataSource dataSource = Objects.requireNonNull(jdbcTemplate.getDataSource());
		return chunk -> {
			for (String item : chunk) {
				if ("2".equals(item)) {
					// Simulate JPA/Hibernate flush failure: mark the underlying
					// connection
					// holder as rollback-only (global), exactly as Hibernate does via
					// JdbcResourceLocalTransactionCoordinatorImpl after a flush error.
					ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager
						.getResource(dataSource);
					if (holder != null) {
						holder.setRollbackOnly();
					}
					throw new RuntimeException("Simulated JPA flush failure for item: " + item);
				}
				jdbcTemplate.update("INSERT INTO delivery (item_number) VALUES (?)", item);
			}
		};
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5377
	@Test
	void testUnexpectedRollbackExceptionPreventedInSequentialScanMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				GlobalRollbackOnlySequentialStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		// Without the fix in doExecute(), the scan transaction for item "2" is globally
		// rollback-only (ConnectionHolder.setRollbackOnly()) but not locally
		// rollback-only.
		// AbstractPlatformTransactionManager.commit() would then call
		// processRollback(defStatus, unexpected=true) and throw
		// UnexpectedRollbackException,
		// causing the step to fail. With the fix, doExecute() calls
		// transactionStatus.setRollbackOnly() before returning from the lambda so that
		// the transaction manager performs a clean rollback and the step completes
		// normally.
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(3, stepExecution.getReadCount());
		Assertions.assertEquals(2, stepExecution.getWriteCount());
		Assertions.assertEquals(1, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(2, stepExecution.getRollbackCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '3'", Integer.class));
		Assertions.assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	// Issue https://github.com/spring-projects/spring-batch/issues/5377
	@Test
	void testUnexpectedRollbackExceptionPreventedInConcurrentScanMode() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				GlobalRollbackOnlyConcurrentStepConfiguration.class);
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
		Assertions.assertEquals(2, stepExecution.getWriteCount());
		Assertions.assertEquals(1, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(2, stepExecution.getRollbackCount());
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '1'", Integer.class));
		Assertions.assertEquals(1,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery WHERE item_number = '3'", Integer.class));
		Assertions.assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "delivery"));
	}

	@Configuration
	static class GlobalRollbackOnlySequentialStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(globalRollbackOnlyWriter(jdbcTemplate))
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	@Configuration
	static class GlobalRollbackOnlyConcurrentStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(globalRollbackOnlyWriter(jdbcTemplate))
				.transactionManager(transactionManager)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	@Configuration
	static class JpaLikeRollbackOnlySequentialStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(jpaLikeWriter(jdbcTemplate))
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	@Configuration
	static class JpaLikeRollbackOnlyConcurrentStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(jpaLikeWriter(jdbcTemplate))
				.transactionManager(transactionManager)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

	@Configuration
	static class TrackingWriterStepConfiguration {

		private final List<List<String>> writerCalls = new CopyOnWriteArrayList<>();

		public List<List<String>> getWriterCalls() {
			return writerCalls;
		}

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			List<String> items = List.of("1", "2", "3");
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 3).reader(new ListItemReader<>(items))
				.writer(chunk -> {
					// Track each writer call with the items it received
					writerCalls.add(new ArrayList<>(chunk.getItems()));

					for (String item : chunk) {
						if ("2".equals(item) || "3".equals(item)) {
							throw new RuntimeException("Simulated write error for item: " + item);
						}
					}
				})
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.build();
		}

	}

}
