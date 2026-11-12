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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for the interaction between the {@link ItemProcessor} and chunk scanning in
 * {@link ChunkOrientedStep}.
 * <p>
 * By default, the processor is transactional: it is re-invoked for every item that is
 * re-attempted while scanning a chunk. This matters when the writer mutates the item it
 * is given (a JPA entity whose flush failed, for example): re-writing the output produced
 * in the rolled back transaction would fail again for an unrelated reason, masking the
 * original failure or failing the step outright.
 *
 * @author Mahmoud Ben Hassine
 * @see <a href="https://github.com/spring-projects/spring-batch/issues/5490">issue
 * 5490</a>
 */
class ChunkOrientedStepScanModeProcessorTests {

	/** Thrown by the writer on the first write attempt of the invalid item. */
	static class BusinessValidationException extends RuntimeException {

		BusinessValidationException(String message) {
			super(message);
		}

	}

	/**
	 * Thrown by the writer when it is given an output item that a previous, failed write
	 * has already mutated. Stands in for the {@code PropertyValueException} Hibernate
	 * raises for an entity whose generated id was assigned by a flush that then failed.
	 */
	static class StaleOutputException extends RuntimeException {

		StaleOutputException(String message) {
			super(message);
		}

	}

	/** A mutable processor output, standing in for a JPA entity. */
	static class Output {

		private final int id;

		private boolean touchedByFailedWrite;

		Output(int id) {
			this.id = id;
		}

	}

	static class CountingProcessor implements ItemProcessor<Integer, Output> {

		private final AtomicInteger invocations = new AtomicInteger();

		@Override
		public Output process(Integer item) {
			this.invocations.incrementAndGet();
			return new Output(item);
		}

	}

	/**
	 * Fails the write of item 2 with a {@link BusinessValidationException}, mutating the
	 * output item as it does so. A second write attempt of that same output item fails
	 * with a {@link StaleOutputException} instead.
	 */
	static class FailingWriter implements ItemWriter<Output> {

		private final List<Integer> written = new CopyOnWriteArrayList<>();

		@Override
		public void write(Chunk<? extends Output> chunk) {
			for (Output output : chunk) {
				if (output.id == 2) {
					if (output.touchedByFailedWrite) {
						throw new StaleOutputException("stale output for item " + output.id);
					}
					output.touchedByFailedWrite = true;
					throw new BusinessValidationException("invalid item " + output.id);
				}
				this.written.add(output.id);
			}
		}

	}

	static class RecordingSkipListener implements SkipListener<Integer, Output> {

		private final List<Throwable> writeSkips = new CopyOnWriteArrayList<>();

		@Override
		public void onSkipInWrite(Output item, Throwable t) {
			this.writeSkips.add(t);
		}

	}

	@Test
	void testProcessorIsReInvokedDuringScanByDefault() throws Exception {
		// given
		CountingProcessor processor = new CountingProcessor();
		FailingWriter writer = new FailingWriter();
		RecordingSkipListener skipListener = new RecordingSkipListener();

		// when
		JobExecution jobExecution = run(step(processor, writer, skipListener, false, false));

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		// 3 items in the first chunk, then 3 more while scanning the rolled back chunk
		assertEquals(6, processor.invocations.get());
		// the skip listener sees the original business exception, not the exception
		// raised
		// by re-writing a stale output item
		assertEquals(1, skipListener.writeSkips.size());
		assertInstanceOf(BusinessValidationException.class, skipListener.writeSkips.get(0));
		assertEquals(2, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertEquals(List.of(1, 1, 3), writer.written);
	}

	@Test
	void testProcessorIsReInvokedDuringScanByDefaultWithConcurrentProcessing() throws Exception {
		// given
		CountingProcessor processor = new CountingProcessor();
		FailingWriter writer = new FailingWriter();
		RecordingSkipListener skipListener = new RecordingSkipListener();

		// when
		JobExecution jobExecution = run(step(processor, writer, skipListener, false, true));

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertEquals(6, processor.invocations.get());
		assertEquals(1, skipListener.writeSkips.size());
		assertInstanceOf(BusinessValidationException.class, skipListener.writeSkips.get(0));
		assertEquals(2, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	void testProcessorIsNotReInvokedDuringScanWhenNonTransactional() throws Exception {
		// given
		CountingProcessor processor = new CountingProcessor();
		FailingWriter writer = new FailingWriter();
		RecordingSkipListener skipListener = new RecordingSkipListener();

		// when
		JobExecution jobExecution = run(step(processor, writer, skipListener, true, false));

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		// each item is processed exactly once: the cached output is re-used while
		// scanning
		assertEquals(3, processor.invocations.get());
		// and because the cached output of item 2 was mutated by the failed write, the
		// skip is reported with the downstream exception instead of the business one
		assertEquals(1, skipListener.writeSkips.size());
		assertInstanceOf(StaleOutputException.class, skipListener.writeSkips.get(0));
		assertEquals(2, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	void testStepCompletesWithNarrowSkipListWhenProcessorIsTransactional() throws Exception {
		// given
		CountingProcessor processor = new CountingProcessor();
		FailingWriter writer = new FailingWriter();
		RecordingSkipListener skipListener = new RecordingSkipListener();

		// when: only the business exception is skippable, which is what a real
		// configuration looks like
		JobExecution jobExecution = run(new ChunkOrientedStepBuilder<Integer, Output>("step", jobRepository(), 3)
			.reader(new ListItemReader<>(List.of(1, 2, 3)))
			.processor(processor)
			.writer(writer)
			.transactionManager(this.transactionManager)
			.faultTolerant()
			.skip(BusinessValidationException.class)
			.skipLimit(10)
			.skipListener(skipListener)
			.build());

		// then: re-processing produces a fresh output item, so the second write attempt
		// fails with the skippable business exception again rather than with an
		// unrelated, non-skippable one
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(1, skipListener.writeSkips.size());
		assertInstanceOf(BusinessValidationException.class, skipListener.writeSkips.get(0));
		assertEquals(2, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	void testStepFailsWithNarrowSkipListWhenProcessorIsNonTransactional() throws Exception {
		// given
		CountingProcessor processor = new CountingProcessor();
		FailingWriter writer = new FailingWriter();
		RecordingSkipListener skipListener = new RecordingSkipListener();

		// when
		JobExecution jobExecution = run(new ChunkOrientedStepBuilder<Integer, Output>("step", jobRepository(), 3)
			.reader(new ListItemReader<>(List.of(1, 2, 3)))
			.processor(processor)
			.writer(writer)
			.transactionManager(this.transactionManager)
			.faultTolerant()
			.processorNonTransactional()
			.skip(BusinessValidationException.class)
			.skipLimit(10)
			.skipListener(skipListener)
			.build());

		// then: re-writing the stale cached output raises an exception that is not
		// skippable, so the scan cannot complete
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(Collections.emptyList(), skipListener.writeSkips);
	}

	private Step step(CountingProcessor processor, FailingWriter writer, RecordingSkipListener skipListener,
			boolean processorNonTransactional, boolean concurrent) {
		ChunkOrientedStepBuilder<Integer, Output> builder = new ChunkOrientedStepBuilder<Integer, Output>("step",
				jobRepository(), 3)
			.reader(new ListItemReader<>(new ArrayList<>(List.of(1, 2, 3))))
			.processor(processor)
			.writer(writer)
			.transactionManager(this.transactionManager)
			.faultTolerant()
			.skipPolicy((throwable, skipCount) -> true)
			.skipListener(skipListener);
		if (processorNonTransactional) {
			builder = builder.processorNonTransactional();
		}
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
		JobOperator jobOperator = this.context.getBean(JobOperator.class);
		return jobOperator.start(job, new JobParameters());
	}

	@Configuration
	@EnableBatchProcessing
	static class BatchConfiguration {

	}

}
