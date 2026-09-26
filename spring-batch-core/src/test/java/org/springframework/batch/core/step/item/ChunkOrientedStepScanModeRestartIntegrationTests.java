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

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for restarting a {@link ChunkOrientedStep} that failed while scanning a chunk.
 * <p>
 * While a chunk is being scanned, each item is re-attempted and committed in its own
 * transaction, but the reader has already read the whole chunk. A restart after a failure
 * during the scan must resume right after the items whose outcome was committed, and not
 * after the last item read.
 *
 * @author Kim ByeongChang
 */
class ChunkOrientedStepScanModeRestartIntegrationTests {

	/** Items failing on write with a skippable exception. */
	private static final Set<String> SKIPPABLE_WRITE_FAILURES = ConcurrentHashMap.newKeySet();

	/** Items failing on write with a non-skippable exception. */
	private static final Set<String> NON_SKIPPABLE_WRITE_FAILURES = ConcurrentHashMap.newKeySet();

	/** Items failing on read with a skippable exception. */
	private static final Set<String> READ_FAILURES = ConcurrentHashMap.newKeySet();

	/** Items failing on read with a non-skippable exception. */
	private static final Set<String> NON_SKIPPABLE_READ_FAILURES = ConcurrentHashMap.newKeySet();

	/**
	 * Items failing once on read with a transient exception, without moving past them.
	 */
	private static final Set<String> TRANSIENT_READ_FAILURES = ConcurrentHashMap.newKeySet();

	/** Items of the read skips reported to the skip listener. */
	private static final List<String> REPORTED_READ_SKIPS = new CopyOnWriteArrayList<>();

	@BeforeEach
	void clearFailures() {
		SKIPPABLE_WRITE_FAILURES.clear();
		NON_SKIPPABLE_WRITE_FAILURES.clear();
		READ_FAILURES.clear();
		NON_SKIPPABLE_READ_FAILURES.clear();
		TRANSIENT_READ_FAILURES.clear();
		REPORTED_READ_SKIPS.clear();
	}

	@Test
	void testRestartAfterFailureDuringScanResumesAfterScannedItems() throws Exception {
		// given: in the single chunk [1..5], 2 and 4 fail on write but only one skip is
		// allowed, so the scan writes 1, skips 2, writes 3 and fails the step on 4
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("2", "4"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				SequentialStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution secondExecution = restart(context, firstExecution);

		// then
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(List.of("1", "3"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "3", "4", "5"), deliveredItems(context));
		assertEquals(2, secondExecution.getStepExecutions().iterator().next().getWriteCount());
	}

	@Test
	void testRestartAfterFailureDuringScanResumesAfterScannedItemsInConcurrentMode() throws Exception {
		// given
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("2", "4"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution secondExecution = restart(context, firstExecution);

		// then
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "3", "4", "5"), deliveredItems(context));
	}

	@Test
	void testRestartAfterFailureDuringScanOfAChunkWithAReadSkip() throws Exception {
		// given: in the single chunk [1..6], reading 2 fails and is skipped, then 3 and
		// 5 fail on write while only two skips are allowed in total, so the scan writes
		// 1, skips 3, writes 4 and fails the step on 5
		READ_FAILURES.add("2");
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("3", "5"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ReadSkipStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution secondExecution = restart(context, firstExecution);

		// then: the read of 2 is skipped again during replay, so the restart resumes
		// with 5, without writing 1 or 4 twice
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(List.of("1", "4"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "4", "5", "6"), deliveredItems(context));
		// and the replayed reads are neither counted nor reported to the skip listener
		StepExecution restartedStep = secondExecution.getStepExecutions().iterator().next();
		assertEquals(2, restartedStep.getReadCount());
		assertEquals(0, restartedStep.getReadSkipCount());
		assertEquals(List.of("2"), REPORTED_READ_SKIPS);
	}

	@Test
	void testRestartAfterFailureDuringScanOfAChunkWithARetriedRead() throws Exception {
		// given: same as the first test, but the first attempt to read 2 fails with a
		// transient exception that does not move the reader past 2, and the read is
		// retried
		TRANSIENT_READ_FAILURES.add("2");
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("2", "4"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				RetriedReadStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution secondExecution = restart(context, firstExecution);

		// then: the retried read counts as a single read, so the restart resumes with 4
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(List.of("1", "3"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "3", "4", "5"), deliveredItems(context));
	}

	@Test
	void testRestartFailsWhenAReplayedReadFailsWithANonSkippableException() throws Exception {
		// given: same as the first test, but reading 1 fails with a non-skippable
		// exception on restart, during replay
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("2", "4"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				SequentialStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		SKIPPABLE_WRITE_FAILURES.clear();
		NON_SKIPPABLE_READ_FAILURES.add("1");
		JobExecution secondExecution = restart(context, firstExecution);

		// then: the failure is not ignored, the restart fails without writing anything
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(BatchStatus.FAILED, secondExecution.getStatus());
		assertEquals(List.of("1", "3"), deliveredItems(context));
	}

	@Test
	void testRestartAfterFailuresDuringScanInTwoSuccessiveExecutions() throws Exception {
		// given: items [1..10] in chunks of 5 with a single skip allowed. The first
		// execution writes 1, skips 2, writes 3 and fails on 4 while scanning [1..5]
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("2", "4"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				TenItemsSingleSkipStepConfiguration.class);
		JobExecution firstExecution = start(context);

		// when: the first restart resumes with 4 and, while scanning [4..8], writes 4,
		// skips 5, writes 6 and fails on 7 before any reader checkpoint is saved
		SKIPPABLE_WRITE_FAILURES.clear();
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("5", "7"));
		JobExecution secondExecution = restart(context, firstExecution);
		List<String> deliveredBeforeSecondRestart = deliveredItems(context);
		SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution thirdExecution = restart(context, secondExecution);

		// then: the second restart resumes with 7, without writing 4 or 6 twice
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(BatchStatus.FAILED, secondExecution.getStatus());
		assertEquals(List.of("1", "3", "4", "6"), deliveredBeforeSecondRestart);
		assertEquals(BatchStatus.COMPLETED, thirdExecution.getStatus());
		assertEquals(List.of("1", "10", "3", "4", "6", "7", "8", "9"), deliveredItems(context));
	}

	@Test
	void testRestartAfterFailureDuringScanFollowingAScanThatEndedWithASkip() throws Exception {
		// given: items [1..10] in chunks of 5 with two skips allowed. The scan of [1..5]
		// writes 1 to 4 and ends by skipping 5, in a transaction that is rolled back, so
		// no reader checkpoint is saved. The scan of [6..10] then writes 6, skips 7,
		// writes 8 and fails on 9
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("5", "7", "9"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				TenItemsTwoSkipsStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution secondExecution = restart(context, firstExecution);

		// then: the restart resumes with 9
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(List.of("1", "2", "3", "4", "6", "8"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "10", "2", "3", "4", "6", "8", "9"), deliveredItems(context));
	}

	@Test
	void testRestartAfterFailureInTheChunkFollowingACompletedScan() throws Exception {
		// given: the scan of chunk [1..5] completes (2 is skipped), then chunk [6..10]
		// fails with a non-skippable exception on 8
		SKIPPABLE_WRITE_FAILURES.add("2");
		NON_SKIPPABLE_WRITE_FAILURES.add("8");
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				TwoChunksStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		NON_SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution secondExecution = restart(context, firstExecution);

		// then: the restart resumes with chunk [6..10], as it would without a scan
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(List.of("1", "3", "4", "5"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "10", "3", "4", "5", "6", "7", "8", "9"), deliveredItems(context));
	}

	@Test
	void testRestartAfterFailureDuringScanWithAReaderThatDoesNotSaveItsState() throws Exception {
		// given: same as the first test, but the reader does not save its state and
		// only returns the items that are not delivered yet (a process indicator)
		SKIPPABLE_WRITE_FAILURES.addAll(Set.of("2", "4"));
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ProcessIndicatorStepConfiguration.class);

		// when
		JobExecution firstExecution = start(context);
		List<String> deliveredBeforeRestart = deliveredItems(context);
		int updatesBeforeRestart = context.getBean(ProcessIndicatorItemReader.class).updateCount;
		SKIPPABLE_WRITE_FAILURES.clear();
		JobExecution secondExecution = restart(context, firstExecution);

		// then: the reader is updated once per committed transaction, the scanned
		// items 1 and 3
		assertEquals(2, updatesBeforeRestart);
		// and the reader restarts from its own state (2, 4 and 5), nothing is
		// discarded
		assertEquals(BatchStatus.FAILED, firstExecution.getStatus());
		assertEquals(List.of("1", "3"), deliveredBeforeRestart);
		assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
		assertEquals(List.of("1", "2", "3", "4", "5"), deliveredItems(context));
	}

	private static JobExecution start(ApplicationContext context) throws Exception {
		return context.getBean(JobOperator.class)
			.start(context.getBean(Job.class), new JobParametersBuilder().toJobParameters());
	}

	private static JobExecution restart(ApplicationContext context, JobExecution jobExecution) throws Exception {
		return context.getBean(JobOperator.class).restart(jobExecution);
	}

	private static List<String> deliveredItems(ApplicationContext context) {
		return context.getBean(JdbcTemplate.class)
			.queryForList("SELECT item_number FROM delivery ORDER BY item_number", String.class);
	}

	private static ChunkOrientedStepBuilder<String, String> stepBuilder(JobRepository jobRepository,
			JdbcTransactionManager transactionManager, JdbcTemplate jdbcTemplate, List<String> items, int chunkSize,
			int skipLimit) {
		return stepBuilder(jobRepository, transactionManager, jdbcTemplate, new RestartableListItemReader(items),
				chunkSize, skipLimit);
	}

	private static ChunkOrientedStepBuilder<String, String> stepBuilder(JobRepository jobRepository,
			JdbcTransactionManager transactionManager, JdbcTemplate jdbcTemplate, ItemReader<String> reader,
			int chunkSize, int skipLimit) {
		return new ChunkOrientedStepBuilder<String, String>(jobRepository, chunkSize).reader(reader).writer(chunk -> {
			for (String item : chunk) {
				if (SKIPPABLE_WRITE_FAILURES.contains(item)) {
					throw new IllegalStateException("Simulated skippable write error for item: " + item);
				}
				if (NON_SKIPPABLE_WRITE_FAILURES.contains(item)) {
					throw new IllegalArgumentException("Simulated non-skippable write error for item: " + item);
				}
				jdbcTemplate.update("INSERT INTO delivery (item_number) VALUES (?)", item);
			}
		})
			.transactionManager(transactionManager)
			.faultTolerant()
			.skip(IllegalStateException.class)
			.skipLimit(skipLimit);
	}

	/**
	 * A restartable reader that saves the number of items read in the execution context,
	 * like most readers provided by Spring Batch, and fails on the items in
	 * {@link #READ_FAILURES} after moving past them.
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
			if (index >= this.items.size()) {
				return null;
			}
			String item = this.items.get(index);
			if (READ_FAILURES.contains(item)) {
				throw new IllegalStateException("Simulated read error for item: " + item);
			}
			if (NON_SKIPPABLE_READ_FAILURES.contains(item)) {
				throw new IllegalArgumentException("Simulated non-skippable read error for item: " + item);
			}
			return item;
		}

		@Override
		protected void doOpen() {
		}

		@Override
		protected void doClose() {
		}

	}

	/**
	 * A restartable reader that fails once on the items in
	 * {@link #TRANSIENT_READ_FAILURES} without moving past them, so that the next call
	 * returns the same item.
	 */
	static class NonAdvancingOnFailureItemReader extends ItemStreamSupport implements ItemReader<String> {

		private final List<String> items;

		private int position;

		NonAdvancingOnFailureItemReader(List<String> items) {
			this.items = items;
			setName("nonAdvancingOnFailureItemReader");
		}

		@Override
		public void open(ExecutionContext executionContext) {
			this.position = executionContext.getInt(getExecutionContextKey("position"), 0);
		}

		@Override
		public void update(ExecutionContext executionContext) {
			executionContext.putInt(getExecutionContextKey("position"), this.position);
		}

		@Override
		public synchronized @Nullable String read() {
			if (this.position >= this.items.size()) {
				return null;
			}
			String item = this.items.get(this.position);
			if (TRANSIENT_READ_FAILURES.remove(item)) {
				throw new TransientDataAccessResourceException("Simulated transient read error for item: " + item);
			}
			this.position++;
			return item;
		}

	}

	/**
	 * A reader that does not save its state: on open, it reads the items that are not
	 * delivered yet, like a reader relying on a process indicator.
	 */
	static class ProcessIndicatorItemReader extends AbstractItemCountingItemStreamItemReader<String> {

		private final List<String> items;

		private final JdbcTemplate jdbcTemplate;

		private List<String> undelivered = List.of();

		private int updateCount;

		ProcessIndicatorItemReader(List<String> items, JdbcTemplate jdbcTemplate) {
			this.items = items;
			this.jdbcTemplate = jdbcTemplate;
			setName("processIndicatorItemReader");
			setSaveState(false);
		}

		@Override
		public void update(ExecutionContext executionContext) {
			super.update(executionContext);
			this.updateCount++;
		}

		@Override
		protected void doOpen() {
			List<String> delivered = this.jdbcTemplate.queryForList("SELECT item_number FROM delivery", String.class);
			this.undelivered = this.items.stream().filter(item -> !delivered.contains(item)).toList();
		}

		@Override
		protected @Nullable String doRead() {
			int index = getCurrentItemCount() - 1;
			return index < this.undelivered.size() ? this.undelivered.get(index) : null;
		}

		@Override
		protected void doClose() {
		}

	}

	@Configuration
	static class SequentialStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate, List.of("1", "2", "3", "4", "5"), 5, 1)
				.build();
		}

	}

	@Configuration
	static class RetriedReadStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate,
					new NonAdvancingOnFailureItemReader(List.of("1", "2", "3", "4", "5")), 5, 1)
				.retryPolicy(RetryPolicy.builder()
					.includes(TransientDataAccessResourceException.class)
					.maxRetries(1)
					.delay(Duration.ZERO)
					.build())
				.build();
		}

	}

	@Configuration
	static class ProcessIndicatorStepConfiguration {

		@Bean
		public ProcessIndicatorItemReader reader(JdbcTemplate jdbcTemplate) {
			return new ProcessIndicatorItemReader(List.of("1", "2", "3", "4", "5"), jdbcTemplate);
		}

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate, ProcessIndicatorItemReader reader) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate, reader, 5, 1).build();
		}

	}

	private static final List<String> TEN_ITEMS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

	@Configuration
	static class TenItemsSingleSkipStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate, TEN_ITEMS, 5, 1).build();
		}

	}

	@Configuration
	static class TenItemsTwoSkipsStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate, TEN_ITEMS, 5, 2).build();
		}

	}

	@Configuration
	static class ConcurrentStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate, List.of("1", "2", "3", "4", "5"), 5, 1)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
		}

	}

	@Configuration
	static class ReadSkipStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate, List.of("1", "2", "3", "4", "5", "6"),
					6, 2)
				.skipListener(new SkipListener<String, String>() {
					@Override
					public void onSkipInRead(Throwable t) {
						REPORTED_READ_SKIPS.add(t.getMessage().substring(t.getMessage().lastIndexOf(' ') + 1));
					}
				})
				.build();
		}

	}

	@Configuration
	static class TwoChunksStepConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate) {
			return stepBuilder(jobRepository, transactionManager, jdbcTemplate,
					List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), 5, 5)
				.build();
		}

	}

}
