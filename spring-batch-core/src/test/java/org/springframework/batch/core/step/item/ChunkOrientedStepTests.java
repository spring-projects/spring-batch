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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.FatalStepExecutionException;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableProcessException;
import org.springframework.batch.infrastructure.item.*;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemWriter;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mahmoud Ben Hassine
 * @author Andrey Litvitski
 * @author xeounxzxu
 */
public class ChunkOrientedStepTests {

	@Test
	void testInheritedPropertiesOnBuild() {
		ChunkOrientedStep<String, String> step = new StepBuilder("step", new ResourcelessJobRepository())
			.<String, String>chunk(5)
			.reader(new ListItemReader<>(List.of("foo", "bar")))
			.writer(items -> {
			})
			// inherited properties from StepBuilderHelper
			.allowStartIfComplete(true)
			.startLimit(5)
			.build();

		Assertions.assertTrue(step.isAllowStartIfComplete());
		Assertions.assertEquals(5, step.getStartLimit());
	}

	@Test
	void testFaultTolerantChunkOrientedStepSetupWithDefaultSkipLimit() {
		Assertions.assertDoesNotThrow(() -> new StepBuilder(mock()).chunk(5)
			.reader(new ListItemReader<>(List.of("item1", "item2")))
			.writer(items -> {
			})
			.faultTolerant()
			.skip(Exception.class)
			.build());
	}

	@Test
	void testFaultTolerantChunkOrientedStepSetupWithDefaultRetryLimit() {
		Assertions.assertDoesNotThrow(() -> new StepBuilder(mock()).chunk(5)
			.reader(new ListItemReader<>(List.of("item1", "item2")))
			.writer(items -> {
			})
			.faultTolerant()
			.retry(Exception.class)
			.build());
	}

	@Test
	void testReadNoMoreThanAvailableItemsInSequentialMode() throws Exception {
		// given
		ItemReader<String> reader = mock();
		ItemWriter<String> writer = chunk -> {
		};
		JobRepository jobRepository = new ResourcelessJobRepository();
		when(reader.read()).thenReturn("1", "2", "3", "4", "5", null);
		ChunkOrientedStep<String, String> step = new ChunkOrientedStep<>("step", 10, reader, writer, jobRepository);
		step.afterPropertiesSet();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		verify(reader, times(6)).read();
	}

	@Test
	void testReadNoMoreThanAvailableItemsInConcurrentMode() throws Exception {
		// given
		ItemReader<String> reader = mock();
		ItemWriter<String> writer = chunk -> {
		};
		JobRepository jobRepository = new ResourcelessJobRepository();
		when(reader.read()).thenReturn("1", "2", "3", "4", "5", null);
		ChunkOrientedStep<String, String> step = new ChunkOrientedStep<>("step", 10, reader, writer, jobRepository);
		step.setTaskExecutor(new SimpleAsyncTaskExecutor());
		step.afterPropertiesSet();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		verify(reader, times(6)).read();
	}

	@Test
	void testRetryLimitWithoutRetryDoesNotRetryErrors() throws Exception {
		// Given: ItemProcessor that throws OutOfMemoryError
		AtomicInteger attempts = new AtomicInteger(0);
		ItemProcessor<String, String> processor = item -> {
			attempts.incrementAndGet();
			throw new OutOfMemoryError("Simulated OOM");
		};

		ChunkOrientedStep<String, String> step = new ChunkOrientedStepBuilder<String, String>(
				new ResourcelessJobRepository(), 2)
			.reader(new ListItemReader<>(List.of("item1")))
			.processor(processor)
			.writer(items -> {
			})
			.transactionManager(new ResourcelessTransactionManager())
			.faultTolerant()
			.retryLimit(3)
			.build();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then: Should fail immediately without retry
		assertEquals(1, attempts.get(),
				"OutOfMemoryError should not be retried. Expected 1 attempt, but got " + attempts.get());
	}

	@Test
	void testRetryLimitWithoutRetryRetriesExceptions() throws Exception {
		// Given: ItemProcessor that fails first 2 times with Exception
		AtomicInteger attempts = new AtomicInteger(0);
		ItemProcessor<String, String> processor = item -> {
			if (attempts.incrementAndGet() < 3) {
				throw new RuntimeException("Temporary failure");
			}
			return item.toUpperCase();
		};
		ListItemReader<String> listItemReader = new ListItemReader<>(List.of("item1"));
		ListItemWriter<String> listItemWriter = new ListItemWriter<>();
		ChunkOrientedStep<String, String> step = new ChunkOrientedStepBuilder<String, String>(
				new ResourcelessJobRepository(), 2)
			.reader(listItemReader)
			.processor(processor)
			.writer(listItemWriter)
			.transactionManager(new ResourcelessTransactionManager())
			.faultTolerant()
			.retryLimit(3)
			.build();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// When: Execute step
		// Then: Should succeed after 2 retries
		step.execute(stepExecution);

		// Should have retried 2 times (total 3 attempts)
		assertEquals(3, attempts.get(), "Should retry RuntimeException");
		assertEquals(List.of("ITEM1"), listItemWriter.getWrittenItems(), "Item should be processed successfully");
	}

	@Test
	void testExplicitRetryConfigurationTakesPrecedence() throws Exception {
		// Given: Explicit retry configuration for IllegalStateException only
		AtomicInteger attempts = new AtomicInteger(0);
		ItemProcessor<String, String> processor = item -> {
			attempts.incrementAndGet();
			throw new RuntimeException("This should not be retried");
		};
		ListItemReader<String> listItemReader = new ListItemReader<>(List.of("item1"));
		ListItemWriter<String> listItemWriter = new ListItemWriter<>();

		ChunkOrientedStep<String, String> step = new ChunkOrientedStepBuilder<String, String>(
				new ResourcelessJobRepository(), 2)
			.reader(listItemReader)
			.processor(processor)
			.writer(listItemWriter)
			.transactionManager(new ResourcelessTransactionManager())
			.faultTolerant()
			.retry(IllegalStateException.class)
			.retryLimit(3)
			.build();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// When & Then: Should fail immediately without retry
		// because RuntimeException is not in the explicit retry list
		step.execute(stepExecution);

		// Should not retry (only 1 attempt)
		assertEquals(1, attempts.get(),
				"RuntimeException should not be retried when only IllegalStateException is configured");
	}

	@Test
	void testDoSkipInProcessShouldThrowNonSkippableProcessExceptionWhenSkipPolicyReturnsFalse() throws Exception {
		// given - fault-tolerant step with NeverSkipItemSkipPolicy and retry limit
		ItemReader<String> reader = new ListItemReader<>(List.of("item1", "item2", "item3"));

		ItemProcessor<String, String> processor = item -> {
			if ("item2".equals(item)) {
				throw new RuntimeException("Processing failed for item2");
			}
			return item.toUpperCase();
		};

		ItemWriter<String> writer = chunk -> {
		};

		JobRepository jobRepository = new ResourcelessJobRepository();
		ChunkOrientedStep<String, String> step = new ChunkOrientedStep<>("step", 3, reader, writer, jobRepository);
		step.setItemProcessor(processor);
		step.setFaultTolerant(true);
		step.setRetryPolicy(RetryPolicy.withMaxRetries(1)); // retry once (initial + 1
		// retry)
		step.setSkipPolicy(new NeverSkipItemSkipPolicy()); // never skip
		step.afterPropertiesSet();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when - execute step
		step.execute(stepExecution);

		// then - should fail with FatalStepExecutionException having
		// NonSkippableProcessException as cause
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Throwable throwable = stepExecution.getFailureExceptions().get(0);
		assertInstanceOf(FatalStepExecutionException.class, throwable,
				"Expected FatalStepExecutionException when skip policy rejects skipping");
		Throwable cause = throwable.getCause();
		assertInstanceOf(NonSkippableProcessException.class, cause,
				"Expected NonSkippableProcessException as cause when skip policy rejects skipping");
		assertEquals("Skip policy rejected skipping item", cause.getMessage());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertEquals(0, stepExecution.getProcessSkipCount(), "Process skip count should be 0");
	}

	@Test
	void testSkippableExceptionsTraversal() throws Exception {
		// given
		class SkippableException extends RuntimeException {

		}
		ItemReader<String> reader = new ListItemReader<>(List.of("item1"));
		ItemWriter<String> writer = chunk -> {
			throw new RuntimeException(new SkippableException());
		};

		JobRepository jobRepository = new ResourcelessJobRepository();
		ChunkOrientedStep<String, String> step = new StepBuilder("step", jobRepository).<String, String>chunk(1)
			.reader(reader)
			.writer(writer)
			.faultTolerant()
			.retry(SkippableException.class)
			.retryLimit(1)
			.skip(SkippableException.class)
			.skipLimit(1)
			.build();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when - execute step
		step.execute(stepExecution);

		// then - should skip the exception thrown by the writer
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecutionExitStatus.getExitCode());
		assertEquals(1, stepExecution.getSkipCount());
	}

	@Test
	void testItemStreamUpdateStillOccursWhenChunkRollsBack_bugReproduction() throws Exception {
		// given: tracking stream to capture update invocations
		TrackingItemStream trackingItemStream = new TrackingItemStream();
		ItemReader<String> reader = new ListItemReader<>(List.of("item1"));
		ItemWriter<String> writer = chunk -> {
			throw new RuntimeException("Simulated failure");
		};
		JobRepository jobRepository = new ResourcelessJobRepository();
		ChunkOrientedStep<String, String> step = new ChunkOrientedStep<>("step", 1, reader, writer, jobRepository);
		step.registerItemStream(trackingItemStream);
		step.afterPropertiesSet();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when: execute step (writer causes chunk rollback)
		step.execute(stepExecution);

		// then: due to current bug the stream update count becomes 1 although chunk
		// rolled back
		assertEquals(0, trackingItemStream.getUpdateCount(),
				"ItemStream should not be updated when chunk transaction fails (bug reproduction)");
	}

	private static final class TrackingItemStream implements ItemStream {

		private int updateCount;

		@Override
		public void open(ExecutionContext executionContext) {
		}

		@Override
		public void update(ExecutionContext executionContext) {
			this.updateCount++;
		}

		@Override
		public void close() {
		}

		int getUpdateCount() {
			return this.updateCount;
		}

	}

	@Test
	void testFilterCountAccuracyInConcurrentMode() throws Exception {
		// given
		int itemCount = 10;
		AtomicInteger readCounter = new AtomicInteger(0);

		ItemReader<Integer> reader = () -> {
			int current = readCounter.incrementAndGet();
			return current <= itemCount ? current : null;
		};

		ItemProcessor<Integer, Integer> filteringProcessor = item -> null;

		ItemWriter<Integer> writer = chunk -> {
		};

		JobRepository jobRepository = new ResourcelessJobRepository();
		ChunkOrientedStep<Integer, Integer> step = new ChunkOrientedStep<>("step", 100, reader, writer, jobRepository);
		step.setItemProcessor(filteringProcessor);
		step.setTaskExecutor(new SimpleAsyncTaskExecutor());
		step.afterPropertiesSet();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		assertEquals(itemCount, stepExecution.getFilterCount(), "Race condition detected! Expected " + itemCount
				+ " filtered items, but got " + stepExecution.getFilterCount());
	}

	@Test
	void testFilterCountAccuracyInSequentialMode() throws Exception {
		// given
		int itemCount = 10;
		AtomicInteger readCounter = new AtomicInteger(0);

		ItemReader<Integer> reader = () -> {
			int current = readCounter.incrementAndGet();
			return current <= itemCount ? current : null;
		};

		ItemProcessor<Integer, Integer> filteringProcessor = item -> null;
		ItemWriter<Integer> writer = chunk -> {
		};

		JobRepository jobRepository = new ResourcelessJobRepository();
		ChunkOrientedStep<Integer, Integer> step = new ChunkOrientedStep<>("step", 100, reader, writer, jobRepository);
		step.setItemProcessor(filteringProcessor);
		step.afterPropertiesSet();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		assertEquals(itemCount, stepExecution.getFilterCount(), "Sequential mode should have accurate filter count");
	}

	@Test
	void testProcessSkipCountAccuracyInConcurrentMode() throws Exception {
		// given
		int itemCount = 10;
		AtomicInteger readCounter = new AtomicInteger(0);

		ItemReader<Integer> reader = () -> {
			int current = readCounter.incrementAndGet();
			return current <= itemCount ? current : null;
		};

		ItemProcessor<Integer, Integer> failingProcessor = item -> {
			throw new RuntimeException("Simulated processing failure");
		};

		ItemWriter<Integer> writer = chunk -> {
		};

		JobRepository jobRepository = new ResourcelessJobRepository();
		ChunkOrientedStep<Integer, Integer> step = new ChunkOrientedStep<>("step", 100, reader, writer, jobRepository);
		step.setItemProcessor(failingProcessor);
		step.setTaskExecutor(new SimpleAsyncTaskExecutor());
		step.setFaultTolerant(true);
		step.setRetryPolicy(RetryPolicy.withMaxRetries(1));
		step.setSkipPolicy((throwable, skipCount) -> throwable instanceof RuntimeException);

		step.afterPropertiesSet();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		assertEquals(itemCount, stepExecution.getProcessSkipCount(), "Race condition detected! Expected " + itemCount
				+ " process skips, but got " + stepExecution.getProcessSkipCount());
	}

	@Test
	void testProcessSkipCountAccuracyInSequentialMode() throws Exception {
		// given
		int itemCount = 10;
		AtomicInteger readCounter = new AtomicInteger(0);

		ItemReader<Integer> reader = () -> {
			int current = readCounter.incrementAndGet();
			return current <= itemCount ? current : null;
		};

		ItemProcessor<Integer, Integer> failingProcessor = item -> {
			throw new RuntimeException("Simulated processing failure");
		};

		ItemWriter<Integer> writer = chunk -> {
		};

		JobRepository jobRepository = new ResourcelessJobRepository();
		ChunkOrientedStep<Integer, Integer> step = new ChunkOrientedStep<>("step", 100, reader, writer, jobRepository);
		step.setItemProcessor(failingProcessor);
		step.setFaultTolerant(true);
		step.setRetryPolicy(RetryPolicy.withMaxRetries(1));
		step.setSkipPolicy((throwable, skipCount) -> throwable instanceof RuntimeException);
		step.afterPropertiesSet();

		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		assertEquals(itemCount, stepExecution.getProcessSkipCount(),
				"Sequential mode should have accurate process skip count");
	}

}
