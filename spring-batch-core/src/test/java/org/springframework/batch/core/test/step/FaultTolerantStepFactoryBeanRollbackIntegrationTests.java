/*
 * Copyright 2010-2025 the original author or authors.
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
package org.springframework.batch.core.test.step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.SynchronizedItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link FaultTolerantStepFactoryBean}.
 */
@SpringJUnitConfig(locations = "/simple-job-launcher-context.xml")
class FaultTolerantStepFactoryBeanRollbackIntegrationTests {

	private static final int MAX_COUNT = 1000;

	private final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory;

	private SkipProcessorStub processor;

	private SkipWriterStub writer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JobRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() {

		writer = new SkipWriterStub(jdbcTemplate, "1", "2", "3", "4", "5");
		processor = new SkipProcessorStub(jdbcTemplate);

		factory = new FaultTolerantStepFactoryBean<>();

		factory.setBeanName("stepName");
		factory.setTransactionManager(transactionManager);
		factory.setJobRepository(repository);
		factory.setCommitInterval(3);
		factory.setSkipLimit(10);

		JdbcTestUtils.deleteFromTables(jdbcTemplate, "ERROR_LOG");

	}

	@Test
	void testUpdatesNoRollback() {

		writer.write(Chunk.of("foo", "bar"));
		processor.process("spam");
		assertEquals(3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

		writer.clear();
		processor.clear();
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

	}

	@Test
	@Timeout(value = 30)
	void testMultithreadedSkipInWriter() throws Throwable {

		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(3);
		taskExecutor.setMaxPoolSize(6);
		taskExecutor.setQueueCapacity(0);
		taskExecutor.afterPropertiesSet();
		factory.setTaskExecutor(taskExecutor);

		factory.setSkippableExceptionClasses(Map.of(Exception.class, true));

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = repository.createJobInstance("skipJob", jobParameters);
		JobExecution jobExecution = repository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		for (int i = 0; i < MAX_COUNT; i++) {

			if (i % 100 == 0) {
				logger.info("Starting step: " + i);
			}

			assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

			try {

				ItemReader<String> reader = new SynchronizedItemReader<>(
						new ListItemReader<>(List.of("1", "2", "3", "4", "5")));
				factory.setItemReader(reader);
				writer.clear();
				factory.setItemWriter(writer);
				processor.clear();
				factory.setItemProcessor(processor);

				Step step = factory.getObject();

				StepExecution stepExecution = repository.createStepExecution(factory.getName(), jobExecution);
				step.execute(stepExecution);
				assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

				assertEquals("[]", writer.getCommitted().toString());
				assertEquals("[]", processor.getCommitted().toString());
				List<String> processed = new ArrayList<>(processor.getProcessed());
				Collections.sort(processed);
				assertEquals("[1, 1, 2, 2, 3, 3, 4, 4, 5, 5]", processed.toString());
				assertEquals(5, stepExecution.getSkipCount());

			}
			catch (Throwable e) {
				logger.info("Failed on iteration " + i + " of " + MAX_COUNT);
				throw e;
			}

		}

	}

	private static class SkipWriterStub implements ItemWriter<String> {

		private final Collection<String> failures;

		private final JdbcTemplate jdbcTemplate;

		public SkipWriterStub(JdbcTemplate jdbcTemplate, String... failures) {
			this.failures = Arrays.asList(failures);
			this.jdbcTemplate = jdbcTemplate;
		}

		public List<String> getCommitted() {
			return jdbcTemplate.query("SELECT MESSAGE from ERROR_LOG where STEP_NAME='written'",
					(rs, rowNum) -> rs.getString(1));
		}

		public void clear() {
			JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "ERROR_LOG", "STEP_NAME='written'");
		}

		@Override
		public void write(Chunk<? extends String> items) {
			for (String item : items) {
				jdbcTemplate.update("INSERT INTO ERROR_LOG (MESSAGE, STEP_NAME) VALUES (?, ?)", item, "written");
				checkFailure(item);
			}
		}

		private void checkFailure(String item) {
			if (failures.contains(item)) {
				throw new RuntimeException("Planned failure");
			}
		}

	}

	private static class SkipProcessorStub implements ItemProcessor<String, String> {

		private final Log logger = LogFactory.getLog(getClass());

		private final List<String> processed = new CopyOnWriteArrayList<>();

		private final JdbcTemplate jdbcTemplate;

		public SkipProcessorStub(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		/**
		 * @return the processed
		 */
		public List<String> getProcessed() {
			return processed;
		}

		public List<String> getCommitted() {
			return jdbcTemplate.query("SELECT MESSAGE from ERROR_LOG where STEP_NAME='processed'",
					(rs, rowNum) -> rs.getString(1));
		}

		public void clear() {
			processed.clear();
			JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "ERROR_LOG", "STEP_NAME='processed'");
		}

		@Override
		public @Nullable String process(String item) {
			processed.add(item);
			logger.debug("Processed item: " + item);
			jdbcTemplate.update("INSERT INTO ERROR_LOG (MESSAGE, STEP_NAME) VALUES (?, ?)", item, "processed");
			return item;
		}

	}

}
