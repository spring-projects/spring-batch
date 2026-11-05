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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Tests that a fault-tolerant {@link ChunkOrientedStep} that retries a failed read of a
 * {@link JdbcPagingItemReader} does not lose items: the failed page is read again from
 * the same position, as the reference documentation states, and a restart resumes with
 * the first item that was not written.
 *
 * @author Mahmoud Ben Hassine
 */
class ChunkOrientedStepPagingReaderRetryTests {

	// the page size is 4: item 2 is on the first page and item 6 is in the middle of the
	// second page
	@ParameterizedTest
	@ValueSource(ints = { 2, 6 })
	void failedPageReadIsRetriedWithoutLosingItems(int failingItem) throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(JobConfiguration.class);
			context.registerBean(Scenario.class, () -> new Scenario(failingItem));
			context.refresh();

			JobOperator jobOperator = context.getBean(JobOperator.class);
			Job job = context.getBean(Job.class);

			JobExecution jobExecution = jobOperator.start(job, new JobParameters());

			StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
			Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecution.getExitStatus().getExitCode());
			Assertions.assertEquals(10, stepExecution.getReadCount());
			Assertions.assertEquals(10, stepExecution.getWriteCount());
			Assertions.assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
					context.getBean(RecordingItemWriter.class).written);
		}
	}

	@ParameterizedTest
	@CsvSource({ "2, 4, 5, false", "2, 4, 5, true", "2, 4, 9, false", "6, 4, 9, false", "6, 3, 7, false",
			"6, 7, 8, false" })
	void restartAfterRetriedPageReadResumesWithTheFirstUnwrittenItem(int failingItem, int chunkSize, int failingWrite,
			boolean namedParameters) throws Exception {
		Scenario scenario = new Scenario(failingItem);
		scenario.chunkSize = chunkSize;
		scenario.failingWrite = failingWrite;
		scenario.namedParameters = namedParameters;

		assertRestart(scenario, BatchStatus.FAILED, items(1, failingWrite - 1), items(failingWrite, 10));
	}

	@Test
	void restartAfterStopFollowingARetriedPageReadResumesWithTheFirstUnwrittenItem() throws Exception {
		Scenario scenario = new Scenario(2);
		scenario.stopAfterFirstChunk = true;

		assertRestart(scenario, BatchStatus.STOPPED, items(1, 4), items(5, 10));
	}

	// the read that is skipped takes one place in the first chunk
	@Test
	void restartAfterSkippedPageReadResumesWithTheFirstUnwrittenItem() throws Exception {
		Scenario scenario = new Scenario(2);
		scenario.readFailures = 2;
		scenario.skipFailedReads = true;
		scenario.failingWrite = 5;

		assertRestart(scenario, BatchStatus.FAILED, items(1, 3), items(4, 10));
	}

	@Test
	void failedPageReadDoesNotCountTowardsTheMaximumItemCount() throws Exception {
		Scenario scenario = new Scenario(2);
		scenario.maxItemCount = 4;

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(JobConfiguration.class);
			context.registerBean(Scenario.class, () -> scenario);
			context.refresh();

			JobExecution jobExecution = context.getBean(JobOperator.class)
				.start(context.getBean(Job.class), new JobParameters());

			Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
			Assertions.assertEquals(items(1, 4), context.getBean(RecordingItemWriter.class).written);
		}
	}

	private static void assertRestart(Scenario scenario, BatchStatus firstStatus, List<Integer> writtenFirst,
			List<Integer> writtenOnRestart) throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(JobConfiguration.class);
			context.registerBean(Scenario.class, () -> scenario);
			context.refresh();

			JobOperator jobOperator = context.getBean(JobOperator.class);
			List<Integer> written = context.getBean(RecordingItemWriter.class).written;

			JobExecution jobExecution = jobOperator.start(context.getBean(Job.class), new JobParameters());
			Assertions.assertEquals(firstStatus, jobExecution.getStatus());
			Assertions.assertEquals(writtenFirst, written);
			written.clear();

			JobExecution restartedExecution = jobOperator.restart(jobExecution);
			Assertions.assertEquals(BatchStatus.COMPLETED, restartedExecution.getStatus(),
					String.valueOf(restartedExecution.getAllFailureExceptions()));
			Assertions.assertEquals(writtenOnRestart, written);
		}
	}

	private static List<Integer> items(int first, int last) {
		return IntStream.rangeClosed(first, last).boxed().toList();
	}

	static class Scenario {

		private final int failingItem;

		private int readFailures = 1;

		private boolean skipFailedReads;

		private int chunkSize = 4;

		private int failingWrite;

		private boolean stopAfterFirstChunk;

		private boolean namedParameters;

		private int maxItemCount = Integer.MAX_VALUE;

		Scenario(int failingItem) {
			this.failingItem = failingItem;
		}

	}

	/**
	 * A row mapper that fails on the given item, like a connection lost while the rows of
	 * a page are fetched.
	 */
	static class FailingRowMapper implements RowMapper<Integer> {

		private final int failingItem;

		private final AtomicInteger failures;

		FailingRowMapper(Scenario scenario) {
			this.failingItem = scenario.failingItem;
			this.failures = new AtomicInteger(scenario.readFailures);
		}

		@Override
		public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
			int id = rs.getInt("id");
			if (id == this.failingItem && this.failures.getAndDecrement() > 0) {
				throw new TransientDataAccessResourceException("Connection lost, please retry");
			}
			return id;
		}

	}

	static class RecordingItemWriter implements ItemWriter<Integer> {

		private final List<Integer> written = new ArrayList<>();

		private final int failingWrite;

		private final AtomicBoolean fail = new AtomicBoolean(true);

		private final AtomicBoolean stop;

		RecordingItemWriter(Scenario scenario) {
			this.failingWrite = scenario.failingWrite;
			this.stop = new AtomicBoolean(scenario.stopAfterFirstChunk);
		}

		@Override
		public void write(Chunk<? extends Integer> chunk) {
			if (chunk.getItems().contains(this.failingWrite) && this.fail.getAndSet(false)) {
				throw new IllegalStateException("Unable to write item " + this.failingWrite);
			}
			chunk.forEach(this.written::add);
			if (this.stop.getAndSet(false)) {
				StepSynchronizationManager.getContext().getStepExecution().setTerminateOnly();
			}
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class JobConfiguration {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.addScript(DatabaseType.H2.getProductSchemaDrop())
				.addScript(DatabaseType.H2.getProductSchema())
				.generateUniqueName(true);
			return builder.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public JdbcTemplate jdbcTemplate(DataSource dataSource) {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			jdbcTemplate.execute("create table source_item (id int primary key)");
			for (int id = 1; id <= 10; id++) {
				jdbcTemplate.update("insert into source_item (id) values (?)", id);
			}
			return jdbcTemplate;
		}

		@Bean
		public RecordingItemWriter recordingItemWriter(Scenario scenario) {
			return new RecordingItemWriter(scenario);
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager, DataSource dataSource,
				JdbcTemplate jdbcTemplate, Scenario scenario, RecordingItemWriter recordingItemWriter)
				throws Exception {
			JdbcPagingItemReaderBuilder<Integer> reader = new JdbcPagingItemReaderBuilder<Integer>().name("reader")
				.dataSource(dataSource)
				.selectClause("select id")
				.fromClause("from source_item")
				.sortKeys(Map.of("id", Order.ASCENDING))
				.rowMapper(new FailingRowMapper(scenario))
				.pageSize(4)
				.maxItemCount(scenario.maxItemCount);
			if (scenario.namedParameters) {
				reader.whereClause("id >= :minId").parameterValues(Map.of("minId", 1));
			}
			ChunkOrientedStepBuilder<Integer, Integer> step = new ChunkOrientedStepBuilder<Integer, Integer>(
					jobRepository, scenario.chunkSize)
				.reader(reader.build())
				.writer(recordingItemWriter)
				.transactionManager(transactionManager)
				.faultTolerant()
				.retryPolicy(RetryPolicy.builder()
					.maxRetries(1)
					.delay(Duration.ZERO)
					.includes(TransientDataAccessResourceException.class)
					.build());
			if (scenario.skipFailedReads) {
				step.skip(TransientDataAccessResourceException.class).skipLimit(1);
			}
			return step.build();
		}

	}

}
