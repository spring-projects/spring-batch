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
import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
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
 * the same position, as the reference documentation states.
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
			context.registerBean(FailingOnceRowMapper.class, () -> new FailingOnceRowMapper(failingItem));
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

	/**
	 * A row mapper that fails once, on the given item, like a connection lost while the
	 * rows of a page are fetched.
	 */
	static class FailingOnceRowMapper implements RowMapper<Integer> {

		private final int failingItem;

		private final AtomicBoolean fail = new AtomicBoolean(true);

		FailingOnceRowMapper(int failingItem) {
			this.failingItem = failingItem;
		}

		@Override
		public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
			int id = rs.getInt("id");
			if (id == this.failingItem && this.fail.getAndSet(false)) {
				throw new TransientDataAccessResourceException("Connection lost, please retry");
			}
			return id;
		}

	}

	static class RecordingItemWriter implements ItemWriter<Integer> {

		private final List<Integer> written = new ArrayList<>();

		@Override
		public void write(Chunk<? extends Integer> chunk) {
			chunk.forEach(this.written::add);
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class JobConfiguration {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.addScript("/org/springframework/batch/core/schema-drop-h2.sql")
				.addScript("/org/springframework/batch/core/schema-h2.sql")
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
		public RecordingItemWriter recordingItemWriter() {
			return new RecordingItemWriter();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager, DataSource dataSource,
				JdbcTemplate jdbcTemplate, FailingOnceRowMapper rowMapper, RecordingItemWriter recordingItemWriter)
				throws Exception {
			return new ChunkOrientedStepBuilder<Integer, Integer>(jobRepository, 4)
				.reader(new JdbcPagingItemReaderBuilder<Integer>().name("reader")
					.dataSource(dataSource)
					.selectClause("select id")
					.fromClause("from source_item")
					.sortKeys(Map.of("id", Order.ASCENDING))
					.rowMapper(rowMapper)
					.pageSize(4)
					.build())
				.writer(recordingItemWriter)
				.transactionManager(transactionManager)
				.faultTolerant()
				.retryPolicy(RetryPolicy.builder()
					.maxRetries(1)
					.delay(Duration.ZERO)
					.includes(TransientDataAccessResourceException.class)
					.build())
				.build();
		}

	}

}
