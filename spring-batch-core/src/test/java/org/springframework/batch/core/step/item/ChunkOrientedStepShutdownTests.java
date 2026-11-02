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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies interruption of a worker's chunk step, including a flow wrapper and restart.
 *
 * @author Mahmoud Ben Hassine
 */
class ChunkOrientedStepShutdownTests {

	@TempDir
	Path directory;

	private EmbeddedDatabase database;

	private JdbcTemplate jdbcTemplate;

	private JdbcTransactionManager transactionManager;

	private JobRepository jobRepository;

	private JobInstance jobInstance;

	private ThreadPoolTaskExecutor processorExecutor;

	private Path input;

	private final CountDownLatch chunkRead = new CountDownLatch(1);

	private int shutdownAtRead = 5;

	@BeforeEach
	void setUp() throws Exception {
		this.database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		this.jdbcTemplate = new JdbcTemplate(this.database);
		this.jdbcTemplate.execute("CREATE TABLE OUTPUT_ITEMS (ID INTEGER PRIMARY KEY)");
		this.transactionManager = new JdbcTransactionManager(this.database);
		var factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(this.database);
		factory.setTransactionManager(this.transactionManager);
		factory.afterPropertiesSet();
		this.jobRepository = factory.getObject();
		this.jobInstance = this.jobRepository.createJobInstance("partitionedJob", new JobParameters());
		this.processorExecutor = new ThreadPoolTaskExecutor();
		this.processorExecutor.setCorePoolSize(2);
		this.processorExecutor.setMaxPoolSize(2);
		this.processorExecutor.setQueueCapacity(10);
		this.processorExecutor.initialize();
		this.input = this.directory.resolve("input.txt");
		Files.write(this.input, IntStream.rangeClosed(1, 10).mapToObj(Integer::toString).toList());
	}

	@AfterEach
	void tearDown() {
		this.processorExecutor.destroy();
		this.database.shutdown();
	}

	@Test
	void interruptedWorkerStopsAndCancelsPendingProcessors() throws Exception {
		var started = new CountDownLatch(2);
		var release = new CountDownLatch(1);
		var finished = new CountDownLatch(2);
		Step worker = workerStep(item -> {
			started.countDown();
			try {
				release.await();
				return item;
			}
			finally {
				finished.countDown();
			}
		});
		StepExecution execution = this.jobRepository.createStepExecution(worker.getName(), newJobExecution());
		try {
			interrupt(worker, execution, started);
			assertStopped(execution);
			assertEquals(1, execution.getRollbackCount());
			assertTrue(finished.await(5, TimeUnit.SECONDS), "Running processors should be cancelled");
			assertEquals(List.of(), output());
		}
		finally {
			release.countDown();
		}
	}

	@Test
	void interruptedWorkerFlowStopsBothStepsAndRestartsFromLastCommittedChunk() throws Exception {
		this.shutdownAtRead = 10;
		var started = new CountDownLatch(2);
		var release = new CountDownLatch(1);
		Step worker = workerStep(item -> {
			if (item > 5) {
				started.countDown();
				release.await();
			}
			return item;
		});
		var followingStepRan = new AtomicBoolean();
		Step following = new StepBuilder("following", this.jobRepository).tasklet((contribution, context) -> {
			followingStepRan.set(true);
			return RepeatStatus.FINISHED;
		}, this.transactionManager).build();
		Step flow = new StepBuilder("workerFlow", this.jobRepository)
			.flow(new FlowBuilder<Flow>("flow").start(worker).next(following).build())
			.build();
		JobExecution job = newJobExecution();
		StepExecution outer = this.jobRepository.createStepExecution(flow.getName(), job);
		try {
			interrupt(flow, outer, started);
			assertStopped(outer);
			StepExecution inner = this.jobRepository.getLastStepExecution(this.jobInstance, "worker");
			assertStopped(inner);
			assertEquals(5, inner.getWriteCount());
			assertEquals(1, inner.getRollbackCount());
			assertFalse(followingStepRan.get());
			assertEquals(List.of(1, 2, 3, 4, 5), output());
		}
		finally {
			release.countDown();
		}

		job.setStatus(BatchStatus.STOPPED);
		this.jobRepository.update(job);
		Step restartedFlow = new StepBuilder("workerFlow", this.jobRepository)
			.flow(new FlowBuilder<Flow>("flow").start(workerStep(item -> item)).next(following).build())
			.build();
		StepExecution restarted = this.jobRepository.createStepExecution(restartedFlow.getName(), newJobExecution());
		restartedFlow.execute(restarted);
		assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
		assertTrue(followingStepRan.get());
		assertEquals(IntStream.rangeClosed(1, 10).boxed().toList(), output());
		assertEquals(5, this.jobRepository.getLastStepExecution(this.jobInstance, "worker").getWriteCount());
	}

	@Test
	void processorErrorRemainsAFailure() throws Exception {
		Step worker = workerStep(item -> {
			throw new IllegalStateException("Cannot process item");
		});
		StepExecution execution = this.jobRepository.createStepExecution(worker.getName(), newJobExecution());
		worker.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), execution.getExitStatus().getExitCode());
		assertEquals(List.of(), output());
	}

	private Step workerStep(ItemProcessor<Integer, Integer> processor) {
		var reader = new FlatFileItemReaderBuilder<Integer>().name("numbers")
			.resource(new FileSystemResource(this.input))
			.lineMapper((line, number) -> {
				if (number == this.shutdownAtRead) {
					this.chunkRead.countDown();
				}
				return Integer.valueOf(line);
			})
			.build();
		return new StepBuilder("worker", this.jobRepository).<Integer, Integer>chunk(5)
			.transactionManager(this.transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(chunk -> {
				for (Integer item : chunk) {
					this.jdbcTemplate.update("INSERT INTO OUTPUT_ITEMS VALUES (?)", item);
				}
			})
			.taskExecutor(this.processorExecutor)
			.build();
	}

	private JobExecution newJobExecution() {
		JobExecution execution = this.jobRepository.createJobExecution(this.jobInstance, new JobParameters(),
				new ExecutionContext());
		execution.setStatus(BatchStatus.STARTED);
		this.jobRepository.update(execution);
		return execution;
	}

	private void interrupt(Step step, StepExecution execution, CountDownLatch started) throws Exception {
		var thread = new AtomicReference<Thread>();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var result = executor.submit(() -> {
				thread.set(Thread.currentThread());
				step.execute(execution);
				return null;
			});
			assertTrue(started.await(5, TimeUnit.SECONDS));
			assertTrue(this.chunkRead.await(5, TimeUnit.SECONDS));
			thread.get().interrupt();
			result.get(5, TimeUnit.SECONDS);
		}
		finally {
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
		}
	}

	private void assertStopped(StepExecution execution) {
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		assertEquals(ExitStatus.STOPPED.getExitCode(), execution.getExitStatus().getExitCode());
		StepExecution saved = this.jobRepository.getStepExecution(execution.getId());
		assertEquals(BatchStatus.STOPPED, saved.getStatus());
		assertEquals(ExitStatus.STOPPED.getExitCode(), saved.getExitStatus().getExitCode());
	}

	private List<Integer> output() {
		return this.jdbcTemplate.queryForList("SELECT ID FROM OUTPUT_ITEMS ORDER BY ID", Integer.class);
	}

}
