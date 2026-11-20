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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A listener that marks the chunk transaction rollback-only after the write and does not
 * throw must fail the step. The rolled-back items are written again on restart.
 *
 * @author WooSeung Kim
 */
class ChunkOrientedStepListenerRollbackOnlyIntegrationTests {

	private static final String AFTER_WRITE = "An ItemWriteListener marked the chunk transaction rollback-only after the chunk was written";

	private static final String AFTER_CHUNK = "A ChunkListener marked the chunk transaction rollback-only after the chunk was written";

	private static final String SCAN_AFTER_WRITE = "An ItemWriteListener marked the scanned item transaction rollback-only after the item was written";

	private static final String SCAN_AFTER_CHUNK = "A ChunkListener marked the scanned item transaction rollback-only";

	@Test
	void afterWriteSwallowedRollbackOnlyFailsTheFirstChunk() throws Exception {
		assertSwallowedWrite("after-write-first", List.of("1", "2", "3", "4"), 2, "1", true, false, List.of(),
				List.of("1", "2", "3", "4"));
	}

	@Test
	void afterChunkSwallowedRollbackOnlyFailsTheFirstChunk() throws Exception {
		assertSwallowedChunk("after-chunk-first", List.of("1", "2", "3", "4"), 2, "1", true, false, List.of(),
				List.of("1", "2", "3", "4"));
	}

	@Test
	void afterWriteSwallowedRollbackOnlyFailsTheMiddleChunk() throws Exception {
		assertSwallowedWrite("after-write-middle", List.of("1", "2", "3", "4", "5", "6"), 2, "3", true, false,
				List.of("1", "2"), List.of("1", "2", "3", "4", "5", "6"));
	}

	@Test
	void afterWriteSwallowedRollbackOnlyFailsTheLastChunk() throws Exception {
		assertSwallowedWrite("after-write-last", List.of("1", "2", "3", "4"), 2, "3", true, false, List.of("1", "2"),
				List.of("1", "2", "3", "4"));
	}

	@Test
	void afterWriteSwallowedRollbackOnlyFailsASingleItemChunk() throws Exception {
		assertSwallowedWrite("after-write-one", List.of("1", "2", "3"), 1, "1", true, false, List.of(),
				List.of("1", "2", "3"));
	}

	@Test
	void afterWriteSwallowedRollbackOnlyFailsWithoutFaultTolerance() throws Exception {
		assertSwallowedWrite("after-write-plain", List.of("1", "2", "3", "4"), 2, "1", false, false, List.of(),
				List.of("1", "2", "3", "4"));
	}

	@Test
	void afterChunkSwallowedRollbackOnlyFailsWithoutFaultTolerance() throws Exception {
		assertSwallowedChunk("after-chunk-plain", List.of("1", "2", "3", "4"), 2, "1", false, false, List.of(),
				List.of("1", "2", "3", "4"));
	}

	@Test
	void concurrentAfterWriteSwallowedRollbackOnlyFailsTheStep() throws Exception {
		assertSwallowedWrite("after-write-concurrent", List.of("1", "2", "3", "4"), 2, "1", true, true, List.of(),
				List.of("1", "2", "3", "4"));
	}

	@Test
	void scanAfterWriteSwallowedRollbackOnlyFailsTheStep() throws Exception {
		assertSwallowedScan("scan-after-write", false, false, "1", List.of(), 1);
	}

	@Test
	void scanAfterChunkSwallowedRollbackOnlyFailsTheStep() throws Exception {
		assertSwallowedScan("scan-after-chunk", true, false, "1", List.of(), 1);
	}

	@Test
	void concurrentScanAfterWriteSwallowedRollbackOnlyFailsTheStep() throws Exception {
		assertSwallowedScan("scan-after-write-concurrent", false, true, "1", List.of(), 1);
	}

	@Test
	void scanAfterWriteSwallowedRollbackOnlyFailsAfterAScannedItemIsWritten() throws Exception {
		assertSwallowedScan("scan-after-write-later", false, false, "3", List.of("1"), 0);
	}

	private static void assertSwallowedWrite(String name, List<String> items, int chunkSize, String marker,
			boolean faultTolerant, boolean concurrent, List<String> committed, List<String> expected) throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Infra.class)) {
			JobExecution execution = run(context, name, items, chunkSize, insertingWriter(context),
					swallowWriteWhen(context, marker), faultTolerant, concurrent);
			assertFailedThenRestart(context, execution, AFTER_WRITE, committed, expected);
		}
	}

	private static void assertSwallowedChunk(String name, List<String> items, int chunkSize, String marker,
			boolean faultTolerant, boolean concurrent, List<String> committed, List<String> expected) throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Infra.class)) {
			JobExecution execution = run(context, name, items, chunkSize, insertingWriter(context),
					swallowChunkWhen(context, marker), faultTolerant, concurrent);
			assertFailedThenRestart(context, execution, AFTER_CHUNK, committed, expected);
		}
	}

	/*
	 * The chunk of four items fails to be written because of item 2 and is scanned: the
	 * listener marks the transaction of the scanned item rollback-only.
	 */
	private static void assertSwallowedScan(String name, boolean afterChunk, boolean concurrent, String marker,
			List<String> committed, int restartWriteSkipCount) throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Infra.class)) {
			StepListener listener = afterChunk ? swallowChunkWhen(context, marker) : swallowWriteWhen(context, marker);
			JobExecution execution = run(context, name, List.of("1", "2", "3", "4"), 4,
					insertingWriterThatRejects(context, "2"), listener, true, concurrent);
			JobExecution restarted = assertFailedThenRestart(context, execution,
					afterChunk ? SCAN_AFTER_CHUNK : SCAN_AFTER_WRITE, committed, List.of("1", "3", "4"));
			assertEquals(restartWriteSkipCount, step(restarted).getWriteSkipCount(), dump(context, restarted));
		}
	}

	private static JobExecution assertFailedThenRestart(AnnotationConfigApplicationContext context,
			JobExecution execution, String failure, List<String> committed, List<String> expected) throws Exception {
		String dump = dump(context, execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus(), dump);
		assertEquals(1, execution.getAllFailureExceptions().size(), dump);
		assertEquals(failure,
				NestedExceptionUtils.getMostSpecificCause(execution.getAllFailureExceptions().get(0)).getMessage(),
				dump);
		assertEquals(committed, rows(context), dump);
		assertEquals(committed.size(), reload(context, execution).getWriteCount(), dump);
		JobExecution restarted = restart(context, execution);
		String restartedDump = dump(context, restarted);
		assertEquals(BatchStatus.COMPLETED, restarted.getStatus(), restartedDump);
		assertEquals(expected, rows(context), restartedDump);
		assertEquals(expected.size() - committed.size(), reload(context, restarted).getWriteCount(), restartedDump);
		return restarted;
	}

	private static JobExecution run(AnnotationConfigApplicationContext context, String name, List<String> items,
			int chunkSize, ItemWriter<String> itemWriter, @Nullable StepListener listener, boolean faultTolerant,
			boolean concurrent) throws Exception {
		JobRepository repository = context.getBean(JobRepository.class);
		var builder = new StepBuilder(name, repository).<String, String>chunk(chunkSize)
			.reader(new RestartableReader(items))
			.writer(itemWriter)
			.transactionManager(transactionManager(context));
		if (faultTolerant) {
			builder.faultTolerant().skipLimit(10).skip(IllegalStateException.class);
		}
		if (concurrent) {
			builder.taskExecutor(new SimpleAsyncTaskExecutor());
		}
		if (listener != null) {
			builder.listener(listener);
		}
		Job job = new JobBuilder(name, repository).start(builder.build()).build();
		context.getBean(JobRegistry.class).register(job);
		return context.getBean(JobOperator.class).start(job, new JobParameters());
	}

	private static ItemWriter<String> insertingWriter(AnnotationConfigApplicationContext context) {
		return chunk -> {
			for (String item : chunk) {
				jdbc(context).update("INSERT INTO delivery (item_number) VALUES (?)", item);
			}
		};
	}

	private static ItemWriter<String> insertingWriterThatRejects(AnnotationConfigApplicationContext context,
			String rejected) {
		return chunk -> {
			if (chunk.size() > 1 && chunk.getItems().contains(rejected)) {
				throw new IllegalStateException("cannot write chunk containing " + rejected);
			}
			for (String item : chunk) {
				if (rejected.equals(item)) {
					throw new IllegalStateException("cannot write " + item);
				}
				jdbc(context).update("INSERT INTO delivery (item_number) VALUES (?)", item);
			}
		};
	}

	private static ItemWriteListener<String> swallowWriteWhen(AnnotationConfigApplicationContext context,
			String marker) {
		JdbcTransactionManager tm = transactionManager(context);
		AtomicBoolean once = new AtomicBoolean(true);
		return new ItemWriteListener<>() {
			@Override
			public void afterWrite(Chunk<? extends String> items) {
				if (items.getItems().contains(marker) && once.compareAndSet(true, false)) {
					swallowRollbackOnly(tm);
				}
			}
		};
	}

	private static ChunkListener<String, String> swallowChunkWhen(AnnotationConfigApplicationContext context,
			String marker) {
		JdbcTransactionManager tm = transactionManager(context);
		AtomicBoolean once = new AtomicBoolean(true);
		return new ChunkListener<>() {
			@Override
			public void afterChunk(Chunk<String> chunk) {
				if (chunk.getItems().contains(marker) && once.compareAndSet(true, false)) {
					swallowRollbackOnly(tm);
				}
			}
		};
	}

	private static void swallowRollbackOnly(PlatformTransactionManager tm) {
		try {
			new TransactionTemplate(tm).executeWithoutResult(status -> {
				throw new IllegalStateException("participant failed");
			});
		}
		catch (RuntimeException ignored) {
			// The participating chunk transaction is already rollback-only.
		}
	}

	private static JobExecution restart(AnnotationConfigApplicationContext context, JobExecution execution)
			throws Exception {
		return context.getBean(JobOperator.class).restart(execution);
	}

	private static StepExecution step(JobExecution execution) {
		return execution.getStepExecutions().iterator().next();
	}

	private static StepExecution reload(AnnotationConfigApplicationContext context, JobExecution execution) {
		return context.getBean(JobRepository.class).getStepExecution(step(execution).getId());
	}

	private static JdbcTemplate jdbc(AnnotationConfigApplicationContext context) {
		return context.getBean(JdbcTemplate.class);
	}

	private static JdbcTransactionManager transactionManager(AnnotationConfigApplicationContext context) {
		return context.getBean(JdbcTransactionManager.class);
	}

	private static List<String> rows(AnnotationConfigApplicationContext context) {
		return jdbc(context).queryForList("SELECT item_number FROM delivery ORDER BY id", String.class);
	}

	private static String dump(AnnotationConfigApplicationContext context, JobExecution execution) {
		StepExecution step = execution.getStepExecutions().iterator().next();
		List<String> failures = new ArrayList<>();
		for (Throwable failure : execution.getAllFailureExceptions()) {
			StringBuilder chain = new StringBuilder(failure.toString());
			Throwable cause = failure.getCause();
			while (cause != null && chain.length() < 500) {
				chain.append(" <- ").append(cause);
				cause = cause.getCause();
			}
			failures.add(chain.toString());
		}
		return "status=" + execution.getStatus() + " read=" + step.getReadCount() + " write=" + step.getWriteCount()
				+ " writeSkip=" + step.getWriteSkipCount() + " commit=" + step.getCommitCount() + " rollback="
				+ step.getRollbackCount() + " delivery=" + rows(context) + " failures=" + failures;
	}

	static class RestartableReader extends AbstractItemCountingItemStreamItemReader<String> {

		private final List<String> items;

		RestartableReader(List<String> items) {
			this.items = items;
			setName("restartableReader");
		}

		@Override
		protected @Nullable String doRead() {
			int index = getCurrentItemCount() - 1;
			if (index >= this.items.size()) {
				return null;
			}
			return this.items.get(index);
		}

		@Override
		protected void doOpen() {
		}

		@Override
		protected void doClose() {
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class Infra extends JdbcInfrastructureConfiguration {

		@Bean
		JobRegistry jobRegistry() {
			return new MapJobRegistry();
		}

	}

}
