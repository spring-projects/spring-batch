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
package org.springframework.batch.core.step.item;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for a {@link ChunkOrientedStep} whose chunk transaction is not committed after
 * the state of the step has been saved in it, for example because a transaction
 * synchronization fails in {@code beforeCommit} or because the commit itself fails.
 *
 * @author Mahmoud Ben Hassine
 */
class ChunkOrientedStepCommitFailureIntegrationTests {

	/** Items whose transaction fails in beforeCommit, and is therefore rolled back. */
	private static final Set<String> BEFORE_COMMIT_FAILURES = ConcurrentHashMap.newKeySet();

	/**
	 * Items whose transaction is rolled back, but with an unknown outcome reported by the
	 * transaction manager, see {@link UnknownOutcomeTransactionManager}.
	 */
	private static final Set<String> UNKNOWN_OUTCOMES = ConcurrentHashMap.newKeySet();

	/** Items failing on write with a skippable exception. */
	private static final Set<String> SKIPPABLE_WRITE_FAILURES = ConcurrentHashMap.newKeySet();

	/** Items failing on write with a non-skippable exception. */
	private static final Set<String> NON_SKIPPABLE_WRITE_FAILURES = ConcurrentHashMap.newKeySet();

	private static final AtomicBoolean UNKNOWN_OUTCOME = new AtomicBoolean();

	/** The update of the state of the writer that fails (1 for the first one), if any. */
	private static final AtomicInteger FAILING_WRITER_UPDATE = new AtomicInteger();

	@BeforeEach
	void clearFailures() {
		BEFORE_COMMIT_FAILURES.clear();
		UNKNOWN_OUTCOMES.clear();
		SKIPPABLE_WRITE_FAILURES.clear();
		NON_SKIPPABLE_WRITE_FAILURES.clear();
		UNKNOWN_OUTCOME.set(false);
		FAILING_WRITER_UPDATE.set(0);
	}

	@ParameterizedTest(name = "concurrent = {0}")
	@ValueSource(booleans = { false, true })
	void testRestartAfterBeforeCommitFailure(boolean concurrent) throws Exception {
		// given: the transaction of the chunk [2] fails in beforeCommit
		BEFORE_COMMIT_FAILURES.add("2");
		ApplicationContext context = new AnnotationConfigApplicationContext(StepConfiguration.class);
		Job job = job(context, stepBuilder(context, 1, concurrent));

		// when
		JobExecution firstExecution = start(context, job);
		JobExecution savedFirstExecution = reload(context, firstExecution);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		BEFORE_COMMIT_FAILURES.clear();
		JobExecution secondExecution = restart(context, savedFirstExecution);

		// then: the step failed with the state of the last commit, and can be restarted
		assertEquals(BatchStatus.FAILED, savedFirstExecution.getStatus());
		StepExecution savedStepExecution = savedFirstExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.FAILED, savedStepExecution.getStatus());
		assertEquals(1, savedStepExecution.getWriteCount());
		assertEquals(1, savedStepExecution.getCommitCount());
		assertEquals(1, savedStepExecution.getRollbackCount());
		assertEquals(List.of("1"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "2", "3"), deliveredItems(context));
	}

	@ParameterizedTest(name = "concurrent = {0}")
	@ValueSource(booleans = { false, true })
	void testRestartAfterItemStreamUpdateFailure(boolean concurrent) throws Exception {
		// given: the writer fails to save its state on the commit of the chunk [2], after
		// the reader has saved its own
		FAILING_WRITER_UPDATE.set(2);
		ApplicationContext context = new AnnotationConfigApplicationContext(StepConfiguration.class);
		Job job = job(context, stepBuilder(context, 1, concurrent));

		// when
		JobExecution firstExecution = start(context, job);
		JobExecution savedFirstExecution = reload(context, firstExecution);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		FAILING_WRITER_UPDATE.set(0);
		JobExecution secondExecution = restart(context, savedFirstExecution);

		// then: the reader state of the chunk that was rolled back is not saved
		assertEquals(BatchStatus.FAILED, savedFirstExecution.getStatus());
		assertEquals(List.of("1"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "2", "3"), deliveredItems(context));
	}

	@ParameterizedTest(name = "concurrent = {0}")
	@ValueSource(booleans = { false, true })
	void testRestartAfterBeforeCommitFailureDuringScan(boolean concurrent) throws Exception {
		// given: in the chunk [1, 2, 3], 2 cannot be written, so the chunk is scanned;
		// the scan writes 1 and skips 2, but the transaction of 3 fails in beforeCommit
		SKIPPABLE_WRITE_FAILURES.add("2");
		BEFORE_COMMIT_FAILURES.add("3");
		ApplicationContext context = new AnnotationConfigApplicationContext(StepConfiguration.class);
		Job job = job(context,
				stepBuilder(context, 3, concurrent).faultTolerant().skip(IllegalStateException.class).skipLimit(1));

		// when
		JobExecution firstExecution = start(context, job);
		JobExecution savedFirstExecution = reload(context, firstExecution);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		BEFORE_COMMIT_FAILURES.clear();
		JobExecution secondExecution = restart(context, savedFirstExecution);

		// then: the restart resumes right after the skip of 2
		assertEquals(BatchStatus.FAILED, savedFirstExecution.getStatus());
		assertEquals(List.of("1"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "3"), deliveredItems(context));
	}

	@ParameterizedTest(name = "concurrent = {0}")
	@ValueSource(booleans = { false, true })
	void testRestartIsRefusedAfterUnknownCommitOutcome(boolean concurrent) throws Exception {
		// given: the outcome of the commit of the chunk [2] is unknown
		UNKNOWN_OUTCOMES.add("2");
		ApplicationContext context = new AnnotationConfigApplicationContext(StepConfiguration.class);
		Job job = job(context, stepBuilder(context, 1, concurrent)
			.transactionManager(new UnknownOutcomeTransactionManager(context.getBean(DataSource.class))));

		// when
		JobExecution firstExecution = start(context, job);
		JobExecution savedFirstExecution = reload(context, firstExecution);

		// then: the step is saved as UNKNOWN, which prevents a restart
		assertEquals(BatchStatus.UNKNOWN, savedFirstExecution.getStatus());
		assertEquals(BatchStatus.UNKNOWN, savedFirstExecution.getStepExecutions().iterator().next().getStatus());
		assertEquals(List.of("1"), deliveredItems(context));
		assertThrows(JobRestartException.class, () -> restart(context, savedFirstExecution));
	}

	@ParameterizedTest(name = "concurrent = {0}")
	@ValueSource(booleans = { false, true })
	void testRestartIsRefusedAfterUnknownRollbackOutcome(boolean concurrent) throws Exception {
		// given: the chunk [2] fails, and the outcome of its rollback is unknown
		UNKNOWN_OUTCOMES.add("2");
		NON_SKIPPABLE_WRITE_FAILURES.add("2");
		ApplicationContext context = new AnnotationConfigApplicationContext(StepConfiguration.class);
		Job job = job(context, stepBuilder(context, 1, concurrent)
			.transactionManager(new UnknownOutcomeTransactionManager(context.getBean(DataSource.class))));

		// when
		JobExecution firstExecution = start(context, job);
		JobExecution savedFirstExecution = reload(context, firstExecution);

		// then: the step is saved as UNKNOWN, which prevents a restart
		assertEquals(BatchStatus.UNKNOWN, savedFirstExecution.getStatus());
		assertEquals(BatchStatus.UNKNOWN, savedFirstExecution.getStepExecutions().iterator().next().getStatus());
		assertThrows(JobRestartException.class, () -> restart(context, savedFirstExecution));
	}

	@ParameterizedTest(name = "concurrent = {0}")
	@ValueSource(booleans = { false, true })
	void testFlowStopsAfterUnknownCommitOutcome(boolean concurrent) throws Exception {
		// given: the outcome of the commit of the chunk [2] is unknown, in a flow that
		// goes on with another step when the step fails
		UNKNOWN_OUTCOMES.add("2");
		ApplicationContext context = new AnnotationConfigApplicationContext(StepConfiguration.class);
		JobRepository jobRepository = context.getBean(JobRepository.class);
		AtomicBoolean nextStepExecuted = new AtomicBoolean();
		Step nextStep = new StepBuilder("nextStep", jobRepository).tasklet((contribution, chunkContext) -> {
			nextStepExecuted.set(true);
			return RepeatStatus.FINISHED;
		}, context.getBean(PlatformTransactionManager.class)).build();
		Step step = stepBuilder(context, 1, concurrent)
			.transactionManager(new UnknownOutcomeTransactionManager(context.getBean(DataSource.class)))
			.build();
		Job job = new JobBuilder("job", jobRepository).start(step).on("FAILED").to(nextStep).end().build();

		// when
		JobExecution jobExecution = start(context, job);

		// then: the flow is stopped instead of going on with the next step
		assertEquals(BatchStatus.UNKNOWN, reload(context, jobExecution).getStatus());
		assertFalse(nextStepExecuted.get());
	}

	private static ChunkOrientedStepBuilder<String, String> stepBuilder(ApplicationContext context, int chunkSize,
			boolean concurrent) {
		ChunkOrientedStepBuilder<String, String> stepBuilder = new ChunkOrientedStepBuilder<String, String>("step",
				context.getBean(JobRepository.class), chunkSize)
			.reader(new RestartableListItemReader(List.of("1", "2", "3")))
			.writer(new DeliveryWriter(context.getBean(JdbcTemplate.class)))
			.transactionManager(context.getBean(PlatformTransactionManager.class));
		if (concurrent) {
			stepBuilder.taskExecutor(new SimpleAsyncTaskExecutor());
		}
		return stepBuilder;
	}

	private static Job job(ApplicationContext context, ChunkOrientedStepBuilder<String, String> stepBuilder)
			throws Exception {
		Job job = new JobBuilder("job", context.getBean(JobRepository.class)).start(stepBuilder.build()).build();
		context.getBean(JobRegistry.class).register(job);
		return job;
	}

	private static JobExecution start(ApplicationContext context, Job job) throws Exception {
		return context.getBean(JobOperator.class).start(job, new JobParametersBuilder().toJobParameters());
	}

	/*
	 * Restart from the job execution saved in the job repository, as a new JVM would.
	 */
	private static JobExecution restart(ApplicationContext context, JobExecution jobExecution) throws Exception {
		return context.getBean(JobOperator.class).restart(jobExecution);
	}

	private static JobExecution reload(ApplicationContext context, JobExecution jobExecution) {
		JobExecution savedJobExecution = context.getBean(JobRepository.class).getJobExecution(jobExecution.getId());
		assertEquals(jobExecution.getId(), savedJobExecution.getId());
		return savedJobExecution;
	}

	private static List<String> deliveredItems(ApplicationContext context) {
		return context.getBean(JdbcTemplate.class)
			.queryForList("SELECT item_number FROM delivery ORDER BY id", String.class);
	}

	/**
	 * A restartable reader that saves the number of items read in the execution context.
	 */
	static class RestartableListItemReader extends AbstractItemCountingItemStreamItemReader<String> {

		private final List<String> items;

		RestartableListItemReader(List<String> items) {
			this.items = items;
			setName("restartableListItemReader");
		}

		@Override
		protected @Nullable String doRead() {
			int index = getCurrentItemCount() - 1;
			return index < this.items.size() ? this.items.get(index) : null;
		}

		@Override
		protected void doOpen() {
		}

		@Override
		protected void doClose() {
		}

	}

	/**
	 * A writer that delivers items, and saves its state like a file writer would.
	 */
	static class DeliveryWriter implements ItemStreamWriter<String> {

		private final JdbcTemplate jdbcTemplate;

		private int updateCount;

		DeliveryWriter(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@Override
		public void write(Chunk<? extends String> chunk) {
			for (String item : chunk) {
				if (SKIPPABLE_WRITE_FAILURES.contains(item)) {
					throw new IllegalStateException("Simulated skippable write error for item: " + item);
				}
				this.jdbcTemplate.update("INSERT INTO delivery (item_number) VALUES (?)", item);
				if (BEFORE_COMMIT_FAILURES.contains(item)) {
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void beforeCommit(boolean readOnly) {
							throw new IllegalStateException("Simulated beforeCommit failure for item: " + item);
						}
					});
				}
				if (UNKNOWN_OUTCOMES.contains(item)) {
					UNKNOWN_OUTCOME.set(true);
				}
				if (NON_SKIPPABLE_WRITE_FAILURES.contains(item)) {
					throw new IllegalArgumentException("Simulated non-skippable write error for item: " + item);
				}
			}
		}

		@Override
		public void update(ExecutionContext executionContext) {
			if (++this.updateCount == FAILING_WRITER_UPDATE.get()) {
				throw new ItemStreamException("Simulated failure to save the state of the writer");
			}
		}

	}

	/**
	 * A transaction manager that rolls back the transaction of the items in
	 * {@link #UNKNOWN_OUTCOMES}, whether it is committed or rolled back, but reports its
	 * outcome as unknown.
	 */
	static class UnknownOutcomeTransactionManager extends JdbcTransactionManager {

		UnknownOutcomeTransactionManager(DataSource dataSource) {
			super(dataSource);
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			if (UNKNOWN_OUTCOME.compareAndSet(true, false)) {
				super.doRollback(status);
				throw new TransactionSystemException("Simulated commit failure with an unknown outcome");
			}
			super.doCommit(status);
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
			super.doRollback(status);
			if (UNKNOWN_OUTCOME.compareAndSet(true, false)) {
				throw new TransactionSystemException("Simulated rollback failure with an unknown outcome");
			}
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	@Import(JdbcInfrastructureConfiguration.class)
	static class StepConfiguration {

		// the jobs of the tests are registered when they are built
		@Bean
		public MapJobRegistry jobRegistry() {
			return new MapJobRegistry();
		}

	}

}
