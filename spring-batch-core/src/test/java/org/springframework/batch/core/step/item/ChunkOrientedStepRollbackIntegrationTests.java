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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies rollback and restart with application-style job configurations.
 *
 * @author Mahmoud Ben Hassine
 */
class ChunkOrientedStepRollbackIntegrationTests {

	@Test
	void processorFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ProcessorFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot process item 4");
		}
	}

	@Test
	void writerFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(WriterFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot write chunk containing item 4");
		}
	}

	@Test
	void afterChunkFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(AfterChunkFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot complete chunk containing item 4");
		}
	}

	@Test
	void afterWriteFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(AfterWriteFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot complete write of chunk containing item 4");
		}
	}

	@Test
	void concurrentProcessorFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ConcurrentProcessorFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot process item 4");
		}
	}

	@Test
	void concurrentWriterFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ConcurrentWriterFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot write chunk containing item 4");
		}
	}

	@Test
	void concurrentAfterWriteFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ConcurrentAfterWriteFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot complete write of chunk containing item 4");
		}
	}

	@Test
	void faultTolerantProcessorFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(FaultTolerantProcessorFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot process item 4");
		}
	}

	@Test
	void faultTolerantWriterFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(FaultTolerantWriterFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot write chunk containing item 4");
		}
	}

	@Test
	void faultTolerantAfterChunkFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(FaultTolerantAfterChunkFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot complete chunk containing item 4");
		}
	}

	@Test
	void faultTolerantAfterWriteFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(FaultTolerantAfterWriteFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot complete write of chunk containing item 4");
		}
	}

	@Test
	void concurrentFaultTolerantProcessorFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(
				ConcurrentFaultTolerantProcessorFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot process item 4");
		}
	}

	@Test
	void concurrentFaultTolerantWriterFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(
				ConcurrentFaultTolerantWriterFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot write chunk containing item 4");
		}
	}

	@Test
	void concurrentFaultTolerantAfterWriteFailureRestoresCheckpointAndAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(
				ConcurrentFaultTolerantAfterWriteFailureConfiguration.class)) {
			assertRollbackAndRestart(context, "Cannot complete write of chunk containing item 4");
		}
	}

	@Test
	void scanningPreservesFilteredCountsAndContextReferences() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ScanningConfiguration.class)) {
			assertSuccessfulScan(context, start(context));
		}
	}

	@Test
	void concurrentScanningPreservesFilteredCountsAndContextReferences() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ConcurrentScanningConfiguration.class)) {
			assertSuccessfulScan(context, start(context));
		}
	}

	@Test
	void scanningWithoutSynchronizationPreservesCounts() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(UnsynchronizedScanningConfiguration.class)) {
			assertSuccessfulScan(context, start(context));
		}
	}

	@Test
	void concurrentScanningWithoutSynchronizationPreservesCounts() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(
				ConcurrentUnsynchronizedScanningConfiguration.class)) {
			assertSuccessfulScan(context, start(context));
		}
	}

	@Test
	void scanCheckpointFailureAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ScanCheckpointFailureConfiguration.class)) {
			assertScanCheckpointFailureAndRestart(context);
		}
	}

	@Test
	void concurrentScanCheckpointFailureAllowsRestart() throws Exception {
		try (var context = new AnnotationConfigApplicationContext(ConcurrentScanCheckpointFailureConfiguration.class)) {
			assertScanCheckpointFailureAndRestart(context);
		}
	}

	private static void assertRollbackAndRestart(ApplicationContext context, String expectedFailure) throws Exception {
		JobExecution execution = start(context);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		Throwable failure = NestedExceptionUtils.getMostSpecificCause(execution.getAllFailureExceptions().get(0));
		assertInstanceOf(IllegalStateException.class, failure);
		assertEquals(expectedFailure, failure.getMessage());

		// Reload metadata from the repository, rather than checking the in-memory state.
		StepExecution savedStep = reload(context, execution);
		assertAll(() -> assertEquals(List.of(1, 2), deliveredItems(context)),
				() -> assertEquals(2, savedStep.getExecutionContext().getInt("lastProcessed")),
				() -> assertEquals("keep", savedStep.getExecutionContext().getString("existing")),
				() -> assertFalse(savedStep.getExecutionContext().containsKey("uncommitted")),
				() -> assertEquals(4, savedStep.getReadCount()), () -> assertEquals(2, savedStep.getWriteCount()),
				() -> assertEquals(1, savedStep.getCommitCount()), () -> assertEquals(1, savedStep.getRollbackCount()));

		JobExecution restarted = restart(context, execution);
		assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
		assertEquals(List.of(1, 2, 3, 4), deliveredItems(context));
		assertEquals(4, reload(context, restarted).getExecutionContext().getInt("lastProcessed"));
	}

	private static void assertSuccessfulScan(ApplicationContext context, JobExecution execution) {
		StepExecution savedStep = reload(context, execution);
		assertAll(() -> assertEquals(BatchStatus.COMPLETED, savedStep.getStatus()),
				() -> assertEquals(List.of(2, 4, 6, 8), deliveredItems(context)),
				() -> assertEquals(8, savedStep.getReadCount()), () -> assertEquals(4, savedStep.getWriteCount()),
				() -> assertEquals(2, savedStep.getFilterCount()), () -> assertEquals(2, savedStep.getWriteSkipCount()),
				() -> assertEquals(2, savedStep.getCommitCount()), () -> assertEquals(4, savedStep.getRollbackCount()),
				() -> assertEquals(8, savedStep.getExecutionContext().getInt("lastProcessed")),
				() -> assertSame(context.getBean("checkpointReference", AtomicReference.class).get(),
						execution.getStepExecutions().iterator().next().getExecutionContext()));
	}

	private static void assertScanCheckpointFailureAndRestart(ApplicationContext context) throws Exception {
		JobExecution execution = start(context);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		Throwable failure = NestedExceptionUtils.getMostSpecificCause(execution.getAllFailureExceptions().get(0));
		assertEquals("Cannot commit scan checkpoint", failure.getMessage());
		StepExecution savedStep = reload(context, execution);
		assertAll(() -> assertEquals(List.of(), deliveredItems(context)),
				() -> assertEquals(0, savedStep.getWriteCount()), () -> assertEquals(0, savedStep.getFilterCount()),
				() -> assertEquals(0, savedStep.getCommitCount()), () -> assertEquals(2, savedStep.getRollbackCount()),
				() -> assertFalse(savedStep.getExecutionContext().containsKey("lastProcessed")));
		assertSuccessfulScan(context, restart(context, execution));
	}

	private static JobExecution start(ApplicationContext context) throws Exception {
		return context.getBean(JobOperator.class).start(context.getBean(Job.class), new JobParameters());
	}

	private static JobExecution restart(ApplicationContext context, JobExecution execution) throws Exception {
		JobExecution savedExecution = context.getBean(JobRepository.class).getJobExecution(execution.getId());
		return context.getBean(JobOperator.class).restart(savedExecution);
	}

	private static StepExecution reload(ApplicationContext context, JobExecution execution) {
		return context.getBean(JobRepository.class)
			.getStepExecution(execution.getStepExecutions().iterator().next().getId());
	}

	private static List<Integer> deliveredItems(ApplicationContext context) {
		return context.getBean(JdbcTemplate.class)
			.queryForList("SELECT item_number FROM delivery ORDER BY id", Integer.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class BatchInfrastructure extends JdbcInfrastructureConfiguration {

		@Bean
		Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder("job", jobRepository).start(step).build();
		}

		@Bean
		AtomicBoolean failOnce() {
			return new AtomicBoolean(true);
		}

		@Bean
		AtomicReference<ExecutionContext> checkpointReference() {
			return new AtomicReference<>();
		}

		@Bean
		StepExecutionListener checkpointInitializer(AtomicReference<ExecutionContext> checkpointReference) {
			return new StepExecutionListener() {
				@Override
				public void beforeStep(StepExecution stepExecution) {
					ExecutionContext checkpoint = stepExecution.getExecutionContext();
					checkpoint.putString("existing", "keep");
					checkpointReference.set(checkpoint);
				}
			};
		}

		@Bean
		JdbcBatchItemWriter<Integer> writer(DataSource dataSource) {
			return new JdbcBatchItemWriterBuilder<Integer>().dataSource(dataSource)
				.sql("INSERT INTO delivery (item_number) VALUES (?)")
				.itemPreparedStatementSetter((item, statement) -> statement.setInt(1, item))
				.build();
		}

		@Bean
		@StepScope
		CheckpointingProcessor processor(
				@Value("#{stepExecution.executionContext}") ExecutionContext executionContext) {
			return new CheckpointingProcessor(executionContext);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ProcessorFailureConfiguration extends BatchInfrastructure {

		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				FailingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(checkpointInitializer)
				.build();
		}

		@Bean
		@StepScope
		FailingProcessor failingProcessor(@Value("#{stepExecution.executionContext}") ExecutionContext executionContext,
				AtomicBoolean failOnce) {
			return new FailingProcessor(executionContext, failOnce);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WriterFailureConfiguration extends BatchInfrastructure {

		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingWriter writer, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.writer(writer)
				.listener(checkpointInitializer)
				.build();
		}

		@Bean
		@StepScope
		CheckpointingWriter checkpointingWriter(JdbcBatchItemWriter<Integer> writer,
				@Value("#{stepExecution.executionContext}") ExecutionContext executionContext, AtomicBoolean failOnce) {
			return new CheckpointingWriter(writer, executionContext, failOnce);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AfterChunkFailureConfiguration extends BatchInfrastructure {

		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				FailingChunkListener chunkListener, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(chunkListener)
				.listener(checkpointInitializer)
				.build();
		}

		@Bean
		@StepScope
		FailingChunkListener chunkListener(
				@Value("#{stepExecution.executionContext}") ExecutionContext executionContext, AtomicBoolean failOnce) {
			return new FailingChunkListener(executionContext, failOnce);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AfterWriteFailureConfiguration extends BatchInfrastructure {

		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				FailingItemWriteListener writeListener, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(writeListener)
				.listener(checkpointInitializer)
				.build();
		}

		@Bean
		@StepScope
		FailingItemWriteListener writeListener(
				@Value("#{stepExecution.executionContext}") ExecutionContext executionContext, AtomicBoolean failOnce) {
			return new FailingItemWriteListener(executionContext, failOnce);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentProcessorFailureConfiguration extends ProcessorFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				FailingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(checkpointInitializer)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentWriterFailureConfiguration extends WriterFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingWriter writer, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.writer(writer)
				.listener(checkpointInitializer)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentAfterWriteFailureConfiguration extends AfterWriteFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				FailingItemWriteListener writeListener, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(writeListener)
				.listener(checkpointInitializer)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FaultTolerantProcessorFailureConfiguration extends ProcessorFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				FailingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(checkpointInitializer)
				.faultTolerant()
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FaultTolerantWriterFailureConfiguration extends WriterFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingWriter writer, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.writer(writer)
				.listener(checkpointInitializer)
				.faultTolerant()
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FaultTolerantAfterChunkFailureConfiguration extends AfterChunkFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				FailingChunkListener chunkListener, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(chunkListener)
				.listener(checkpointInitializer)
				.faultTolerant()
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FaultTolerantAfterWriteFailureConfiguration extends AfterWriteFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				FailingItemWriteListener writeListener, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(writeListener)
				.listener(checkpointInitializer)
				.faultTolerant()
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentFaultTolerantProcessorFailureConfiguration extends ProcessorFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				FailingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(checkpointInitializer)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentFaultTolerantWriterFailureConfiguration extends WriterFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingWriter writer, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.writer(writer)
				.listener(checkpointInitializer)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentFaultTolerantAfterWriteFailureConfiguration extends AfterWriteFailureConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				CheckpointingProcessor processor, JdbcBatchItemWriter<Integer> writer,
				FailingItemWriteListener writeListener, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4)))
				.processor(processor)
				.writer(writer)
				.listener(writeListener)
				.listener(checkpointInitializer)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ScanningConfiguration extends BatchInfrastructure {

		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				ScanningProcessor processor, SkippingWriter writer, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(4)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4, 5, 6, 7, 8)))
				.processor(processor)
				.writer(writer)
				.listener(checkpointInitializer)
				.faultTolerant()
				.skip(IllegalStateException.class)
				.skipLimit(2)
				.build();
		}

		@Bean
		@StepScope
		ScanningProcessor scanningProcessor(
				@Value("#{stepExecution.executionContext}") ExecutionContext executionContext) {
			return new ScanningProcessor(executionContext);
		}

		@Bean
		SkippingWriter skippingWriter(JdbcTemplate jdbcTemplate) {
			return new SkippingWriter(jdbcTemplate);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentScanningConfiguration extends ScanningConfiguration {

		@Override
		@Bean
		Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				ScanningProcessor processor, SkippingWriter writer, StepExecutionListener checkpointInitializer) {
			return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(4)
				.transactionManager(transactionManager)
				.reader(new RestartableReader(List.of(1, 2, 3, 4, 5, 6, 7, 8)))
				.processor(processor)
				.writer(writer)
				.listener(checkpointInitializer)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.faultTolerant()
				.skip(IllegalStateException.class)
				.skipLimit(2)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UnsynchronizedScanningConfiguration extends ScanningConfiguration {

		@Override
		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);
			transactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_NEVER);
			return transactionManager;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentUnsynchronizedScanningConfiguration extends ConcurrentScanningConfiguration {

		@Override
		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);
			transactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_NEVER);
			return transactionManager;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ScanCheckpointFailureConfiguration extends ScanningConfiguration {

		@Override
		@Bean
		SkippingWriter skippingWriter(JdbcTemplate jdbcTemplate) {
			return new FailingScanCheckpointWriter(jdbcTemplate);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConcurrentScanCheckpointFailureConfiguration extends ConcurrentScanningConfiguration {

		@Override
		@Bean
		SkippingWriter skippingWriter(JdbcTemplate jdbcTemplate) {
			return new FailingScanCheckpointWriter(jdbcTemplate);
		}

	}

	static class CheckpointingProcessor implements ItemProcessor<Integer, Integer> {

		protected final ExecutionContext executionContext;

		CheckpointingProcessor(ExecutionContext executionContext) {
			this.executionContext = executionContext;
		}

		@Override
		public @Nullable Integer process(Integer item) {
			// Concurrent processors can finish out of order; retain the highest processed
			// id.
			synchronized (this.executionContext) {
				this.executionContext.putInt("lastProcessed",
						Math.max(item, this.executionContext.getInt("lastProcessed", 0)));
			}
			return item;
		}

	}

	static class FailingProcessor extends CheckpointingProcessor {

		private final AtomicBoolean failOnce;

		FailingProcessor(ExecutionContext executionContext, AtomicBoolean failOnce) {
			super(executionContext);
			this.failOnce = failOnce;
		}

		@Override
		public Integer process(Integer item) {
			super.process(item);
			if (item == 4 && this.failOnce.compareAndSet(true, false)) {
				markUncommittedChanges(this.executionContext);
				throw new IllegalStateException("Cannot process item 4");
			}
			return item;
		}

	}

	static class CheckpointingWriter implements ItemWriter<Integer> {

		private final JdbcBatchItemWriter<Integer> delegate;

		private final ExecutionContext executionContext;

		private final AtomicBoolean failOnce;

		CheckpointingWriter(JdbcBatchItemWriter<Integer> delegate, ExecutionContext executionContext,
				AtomicBoolean failOnce) {
			this.delegate = delegate;
			this.executionContext = executionContext;
			this.failOnce = failOnce;
		}

		@Override
		public void write(Chunk<? extends Integer> chunk) throws Exception {
			this.delegate.write(chunk);
			for (Integer item : chunk) {
				this.executionContext.putInt("lastProcessed", item);
			}
			// Fail after executing the inserts, so the business changes must roll back.
			if (chunk.getItems().contains(4) && this.failOnce.compareAndSet(true, false)) {
				markUncommittedChanges(this.executionContext);
				throw new IllegalStateException("Cannot write chunk containing item 4");
			}
		}

	}

	static class FailingChunkListener implements ChunkListener<Integer, Integer> {

		private final ExecutionContext executionContext;

		private final AtomicBoolean failOnce;

		FailingChunkListener(ExecutionContext executionContext, AtomicBoolean failOnce) {
			this.executionContext = executionContext;
			this.failOnce = failOnce;
		}

		@Override
		public void afterChunk(Chunk<Integer> chunk) {
			// This callback runs after writing, inside the chunk transaction.
			if (chunk.getItems().contains(4) && this.failOnce.compareAndSet(true, false)) {
				markUncommittedChanges(this.executionContext);
				throw new IllegalStateException("Cannot complete chunk containing item 4");
			}
		}

	}

	static class FailingItemWriteListener implements ItemWriteListener<Integer> {

		private final ExecutionContext executionContext;

		private final AtomicBoolean failOnce;

		FailingItemWriteListener(ExecutionContext executionContext, AtomicBoolean failOnce) {
			this.executionContext = executionContext;
			this.failOnce = failOnce;
		}

		@Override
		public void afterWrite(Chunk<? extends Integer> items) {
			// This callback runs after the write has been counted, inside the chunk
			// transaction.
			if (items.getItems().contains(4) && this.failOnce.compareAndSet(true, false)) {
				markUncommittedChanges(this.executionContext);
				throw new IllegalStateException("Cannot complete write of chunk containing item 4");
			}
		}

	}

	private static void markUncommittedChanges(ExecutionContext executionContext) {
		executionContext.remove("existing");
		executionContext.putString("uncommitted", "changed");
	}

	static class ScanningProcessor extends CheckpointingProcessor {

		ScanningProcessor(ExecutionContext executionContext) {
			super(executionContext);
		}

		@Override
		public @Nullable Integer process(Integer item) {
			super.process(item);
			return item == 1 || item == 5 ? null : item;
		}

	}

	static class SkippingWriter implements ItemWriter<Integer> {

		private final JdbcTemplate jdbcTemplate;

		SkippingWriter(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@Override
		public void write(Chunk<? extends Integer> chunk) {
			for (Integer item : chunk) {
				this.jdbcTemplate.update("INSERT INTO delivery (item_number) VALUES (?)", item);
				if (item == 3 || item == 7) {
					throw new IllegalStateException("Cannot write item " + item);
				}
			}
		}

	}

	static class FailingScanCheckpointWriter extends SkippingWriter {

		private final AtomicBoolean failOnce = new AtomicBoolean(true);

		FailingScanCheckpointWriter(JdbcTemplate jdbcTemplate) {
			super(jdbcTemplate);
		}

		@Override
		public void write(Chunk<? extends Integer> chunk) {
			super.write(chunk);
			if (chunk.getItems().equals(List.of(2)) && this.failOnce.compareAndSet(true, false)) {
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void beforeCommit(boolean readOnly) {
						throw new IllegalStateException("Cannot commit scan checkpoint");
					}
				});
			}
		}

	}

	static class RestartableReader extends AbstractItemCountingItemStreamItemReader<Integer> {

		private final List<Integer> items;

		RestartableReader(List<Integer> items) {
			this.items = items;
			setName("reader");
		}

		@Override
		protected @Nullable Integer doRead() {
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

}
