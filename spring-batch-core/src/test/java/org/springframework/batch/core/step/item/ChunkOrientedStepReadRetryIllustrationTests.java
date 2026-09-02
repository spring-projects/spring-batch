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
import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Illustrates that a fault-tolerant {@link ChunkOrientedStep} can retry a transient
 * failure on {@link ItemReader#read()} (eg a database connection lost in flight) and
 * successfully recover on a subsequent attempt, without violating the "forward only"
 * reader contract: the failed attempt never returns an item, so nothing is skipped or
 * re-consumed when the retry succeeds.
 *
 * @author Mahmoud Ben Hassine
 */
class ChunkOrientedStepReadRetryIllustrationTests {

	@Test
	void readIsRetriedOnTransientFailureAndSucceedsOnSecondAttempt() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		Assertions.assertEquals(3, stepExecution.getReadCount());
		List<String> written = context.getBean(RecordingItemWriter.class).written;
		Assertions.assertEquals(List.of("foo", "bar", "baz"), written);
	}

	/**
	 * A keyset-based item reader: each call queries for the row right after the last one
	 * successfully returned. Because the cursor position (lastId) is only advanced
	 * *after* a row has been successfully returned, a read attempt that throws before
	 * returning anything leaves the reader positioned exactly where it was: the next
	 * read() attempt (the retry) queries for, and returns, the very same next row.
	 */
	static class FlakyKeysetItemReader implements ItemReader<String> {

		private final JdbcTemplate jdbcTemplate;

		private long lastId = 0;

		private boolean firstCallDone = false;

		FlakyKeysetItemReader(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@Override
		public String read() {
			if (!this.firstCallDone) {
				this.firstCallDone = true;
				// Simulate a transient failure on the very first read attempt, eg the
				// connection was lost while the query was in flight. No row has been
				// consumed yet: lastId is still 0.
				throw new TransientDataAccessResourceException("Connection lost, please retry");
			}
			List<Map<String, Object>> rows = this.jdbcTemplate
				.queryForList("select id, name from source_item where id > ? order by id limit 1", this.lastId);
			if (rows.isEmpty()) {
				return null;
			}
			Map<String, Object> row = rows.get(0);
			this.lastId = ((Number) row.get("ID")).longValue();
			return (String) row.get("NAME");
		}

	}

	static class RecordingItemWriter implements ItemWriter<String> {

		private final List<String> written = new ArrayList<>();

		@Override
		public void write(org.springframework.batch.infrastructure.item.Chunk<? extends String> chunk) {
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
			jdbcTemplate.execute("create table source_item (id int primary key, name varchar(10))");
			jdbcTemplate.update("insert into source_item (id, name) values (1, 'foo')");
			jdbcTemplate.update("insert into source_item (id, name) values (2, 'bar')");
			jdbcTemplate.update("insert into source_item (id, name) values (3, 'baz')");
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
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				JdbcTemplate jdbcTemplate, RecordingItemWriter recordingItemWriter) {
			return new ChunkOrientedStepBuilder<String, String>(jobRepository, 10)
				.reader(new FlakyKeysetItemReader(jdbcTemplate))
				.writer(recordingItemWriter)
				.transactionManager(transactionManager)
				.faultTolerant()
				.retry(TransientDataAccessResourceException.class)
				.retryLimit(3)
				.build();
		}

	}

}
