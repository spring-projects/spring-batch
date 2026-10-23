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
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link ChunkListener} contract while {@link ChunkOrientedStep} is
 * scanning a rolled back chunk.
 *
 * @author Mahmoud Ben Hassine
 * @see <a href="https://github.com/spring-projects/spring-batch/issues/5493">issue
 * 5493</a>
 */
class ChunkOrientedStepScanModeChunkListenerTests {

	/**
	 * Records the runtime type of every item handed to each callback. The step is
	 * declared as {@code <String, Integer>}, so {@code beforeChunk} must only ever see
	 * {@code String} items and {@code afterChunk} only {@code Integer} items.
	 */
	static class RecordingChunkListener implements ChunkListener<String, Integer> {

		final List<String> beforeChunkItemTypes = new CopyOnWriteArrayList<>();

		final List<String> afterChunkItemTypes = new CopyOnWriteArrayList<>();

		final List<Exception> chunkErrors = new CopyOnWriteArrayList<>();

		final List<List<Integer>> chunkErrorChunks = new CopyOnWriteArrayList<>();

		@Override
		public void beforeChunk(Chunk<String> chunk) {
			for (Object item : chunk.getItems()) {
				this.beforeChunkItemTypes.add(item.getClass().getSimpleName());
			}
		}

		@Override
		public void afterChunk(Chunk<Integer> chunk) {
			for (Object item : chunk.getItems()) {
				this.afterChunkItemTypes.add(item.getClass().getSimpleName());
			}
		}

		@Override
		public void onChunkError(Exception exception, Chunk<Integer> chunk) {
			this.chunkErrors.add(exception);
			this.chunkErrorChunks.add(List.copyOf(chunk.getItems()));
		}

	}

	/**
	 * Fails only for the single item chunks handed to it while scanning, so that the step
	 * reaches scan mode before the listener throws.
	 */
	static class ThrowingDuringScanChunkListener extends RecordingChunkListener {

		@Override
		public void beforeChunk(Chunk<String> chunk) {
			super.beforeChunk(chunk);
			if (chunk.size() == 1) {
				throw new IllegalStateException("listener failure during scan");
			}
		}

	}

	@Test
	void testBeforeChunkOnlyReceivesInputItemsDuringScan() throws Exception {
		// given
		RecordingChunkListener listener = new RecordingChunkListener();

		// when
		JobExecution jobExecution = run(step(listener, false));

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// one call for the initial chunk of 3 items, then one call per scanned item,
		// always with input items
		assertEquals(List.of("String", "String", "String", "String", "String", "String"),
				listener.beforeChunkItemTypes);
		// only the item that was actually written reaches afterChunk, as an output item
		assertEquals(List.of("Integer"), listener.afterChunkItemTypes);
	}

	@Test
	void testOnChunkErrorIsCalledForTheWholeChunkAndForEachFaultyScannedItem() throws Exception {
		// given
		RecordingChunkListener listener = new RecordingChunkListener();

		// when
		JobExecution jobExecution = run(step(listener, false));

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// once for the initial chunk whose write failed, then once per scanned item that
		// turned out to be faulty. Item 1 is written successfully and reports no error.
		assertEquals(List.of(List.of(1, 2, 3), List.of(2), List.of(3)), listener.chunkErrorChunks);
	}

	@Test
	void testChunkListenerIsNotCalledInConcurrentStepDuringScan() throws Exception {
		// given
		RecordingChunkListener listener = new RecordingChunkListener();

		// when
		JobExecution jobExecution = run(step(listener, true));

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// the ChunkListener contract states that it is not called in concurrent steps,
		// which must hold for scan transactions as well
		assertEquals(List.of(), listener.beforeChunkItemTypes);
		assertEquals(List.of(), listener.afterChunkItemTypes);
		assertEquals(List.of(), listener.chunkErrors);
		// scanning still runs and skips the two failing items
		assertEquals(1, stepExecution.getWriteCount());
		assertEquals(2, stepExecution.getWriteSkipCount());
	}

	@Test
	void testChunkListenerExceptionDuringScanFailsTheStep() throws Exception {
		// given
		ThrowingDuringScanChunkListener listener = new ThrowingDuringScanChunkListener();

		// when
		JobExecution jobExecution = run(step(listener, false));

		// then: the item polled off the scan queue can no longer be written or skipped,
		// so the step must fail rather than complete with the item unaccounted for
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// only for the initial chunk whose write failed. The scan attempt failed before
		// producing an output item, so there is no chunk to report and the listener is
		// not called with an empty one.
		assertEquals(List.of(List.of(1, 2, 3)), listener.chunkErrorChunks);
		assertTrue(stepExecution.getFailureExceptions()
			.stream()
			.anyMatch(throwable -> throwable.getMessage() != null
					&& throwable.getMessage().contains("Unable to process chunk during scan")));
	}

	@Test
	void testOnChunkErrorReportsTheFaultyItemWhenTheScanCannotSkipIt() throws Exception {
		// given: a skip policy that rejects the write failure, so the scanned item cannot
		// be skipped and the step fails
		RecordingChunkListener listener = new RecordingChunkListener();
		Step step = new ChunkOrientedStepBuilder<String, Integer>("step", jobRepository(), 3)
			.reader(new ListItemReader<>(List.of("1", "2", "3")))
			.processor(Integer::parseInt)
			.writer(chunk -> {
				for (Integer item : chunk) {
					if (item == 2 || item == 3) {
						throw new IllegalStateException("Simulated write error for item: " + item);
					}
				}
			})
			.transactionManager(this.transactionManager)
			.faultTolerant()
			.skipPolicy((throwable, skipCount) -> skipCount < 0)
			.listener(listener)
			.build();

		// when
		JobExecution jobExecution = run(step);

		// then: the failure is reported with the item that could not be written, never
		// with an empty chunk
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(List.of(List.of(1, 2, 3), List.of(2)), listener.chunkErrorChunks);
	}

	@Test
	void testNoItemIsLostWhenTheStepCompletes() throws Exception {
		// given
		RecordingChunkListener listener = new RecordingChunkListener();

		// when
		JobExecution jobExecution = run(step(listener, false));

		// then: every item that was read is accounted for by a write, a skip or a filter
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(stepExecution.getReadCount(),
				stepExecution.getWriteCount() + stepExecution.getWriteSkipCount() + stepExecution.getFilterCount());
	}

	/*
	 * A step of 3 items where the write of items 2 and 3 fails, so that the chunk is
	 * rolled back and scanned one item per transaction. Input and output types differ, to
	 * make a Chunk<I> / Chunk<O> mix-up observable.
	 */
	private Step step(ChunkListener<String, Integer> listener, boolean concurrent) {
		ChunkOrientedStepBuilder<String, Integer> builder = new ChunkOrientedStepBuilder<String, Integer>("step",
				jobRepository(), 3)
			.reader(new ListItemReader<>(List.of("1", "2", "3")))
			.processor(Integer::parseInt)
			.writer(chunk -> {
				for (Integer item : chunk) {
					if (item == 2 || item == 3) {
						throw new IllegalStateException("Simulated write error for item: " + item);
					}
				}
			})
			.transactionManager(this.transactionManager)
			.faultTolerant()
			.skipPolicy(new AlwaysSkipItemSkipPolicy())
			.skipLimit(10)
			.listener(listener);
		if (concurrent) {
			builder = builder.taskExecutor(new SimpleAsyncTaskExecutor());
		}
		return builder.build();
	}

	private final PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	private AnnotationConfigApplicationContext context;

	private JobRepository jobRepository() {
		if (this.context == null) {
			this.context = new AnnotationConfigApplicationContext(BatchConfiguration.class);
		}
		return this.context.getBean(JobRepository.class);
	}

	private JobExecution run(Step step) throws Exception {
		JobRepository jobRepository = jobRepository();
		Job job = new JobBuilder("job", jobRepository).start(step).build();
		return this.context.getBean(JobOperator.class).start(job, new JobParameters());
	}

	@Configuration
	@EnableBatchProcessing
	static class BatchConfiguration {

	}

}
