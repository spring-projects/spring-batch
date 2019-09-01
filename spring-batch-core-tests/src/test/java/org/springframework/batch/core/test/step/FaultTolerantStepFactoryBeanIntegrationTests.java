/*
 * Copyright 2010-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Tests for {@link FaultTolerantStepFactoryBean}.
 */
@ContextConfiguration(locations = "/simple-job-launcher-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class FaultTolerantStepFactoryBeanIntegrationTests {

	private static final int MAX_COUNT = 1000;

	private final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory;

	private SkipProcessorStub processor;

	private SkipWriterStub writer;

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Before
	public void setUp() throws Exception {

		writer = new SkipWriterStub(dataSource);
		processor = new SkipProcessorStub(dataSource);

		factory = new FaultTolerantStepFactoryBean<>();

		factory.setBeanName("stepName");
		factory.setTransactionManager(transactionManager);
		factory.setJobRepository(repository);
		factory.setCommitInterval(3);
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(3);
		taskExecutor.setMaxPoolSize(6);
		taskExecutor.setQueueCapacity(0);
		taskExecutor.afterPropertiesSet();
		factory.setTaskExecutor(taskExecutor);
		
		JdbcTestUtils.deleteFromTables(new JdbcTemplate(dataSource), "ERROR_LOG");

	}
	
	@Test
	public void testUpdatesNoRollback() throws Exception {

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		writer.write(Arrays.asList("foo", "bar"));
		processor.process("spam");
		assertEquals(3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

		writer.clear();
		processor.clear();
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

	}

	@Test
	public void testMultithreadedSunnyDay() throws Throwable {

		jobExecution = repository.createJobExecution("vanillaJob", new JobParameters());

		for (int i = 0; i < MAX_COUNT; i++) {

			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

			SkipReaderStub reader = new SkipReaderStub();
			reader.clear();
			reader.setItems("1", "2", "3", "4", "5");
			factory.setItemReader(reader);
			writer.clear();
			factory.setItemWriter(writer);
			processor.clear();
			factory.setItemProcessor(processor);

			assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

			try {

				Step step = factory.getObject();

				stepExecution = jobExecution.createStepExecution(factory.getName());
				repository.add(stepExecution);
				step.execute(stepExecution);
				assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

				List<String> committed = new ArrayList<>(writer.getCommitted());
				Collections.sort(committed);
				assertEquals("[1, 2, 3, 4, 5]", committed.toString());
				List<String> processed = new ArrayList<>(processor.getCommitted());
				Collections.sort(processed);
				assertEquals("[1, 2, 3, 4, 5]", processed.toString());
				assertEquals(0, stepExecution.getSkipCount());

			}
			catch (Throwable e) {
				logger.info("Failed on iteration " + i + " of " + MAX_COUNT);
				throw e;
			}

		}

	}

	private static class SkipReaderStub implements ItemReader<String> {

		private String[] items;

		private int counter = -1;

		public SkipReaderStub() throws Exception {
			super();
		}

		public void setItems(String... items) {
			Assert.isTrue(counter < 0, "Items cannot be set once reading has started");
			this.items = items;
		}

		public void clear() {
			counter = -1;
		}

		@Nullable
		@Override
		public synchronized String read() throws Exception, UnexpectedInputException, ParseException {
			counter++;
			if (counter >= items.length) {
				return null;
			}
			String item = items[counter];
			return item;
		}
	}

	private static class SkipWriterStub implements ItemWriter<String> {

		private List<String> written = new ArrayList<>();

		private Collection<String> failures = Collections.emptySet();

		private JdbcTemplate jdbcTemplate;

		public SkipWriterStub(DataSource dataSource) {
			jdbcTemplate = new JdbcTemplate(dataSource);
		}

		public List<String> getCommitted() {
			return jdbcTemplate.query("SELECT MESSAGE from ERROR_LOG where STEP_NAME='written'",
					new RowMapper<String>() {
						@Override
						public String mapRow(ResultSet rs, int rowNum) throws SQLException {
							return rs.getString(1);
						}
					});
		}

		public void clear() {
			written.clear();
			jdbcTemplate.update("DELETE FROM ERROR_LOG where STEP_NAME='written'");
		}

		@Override
		public void write(List<? extends String> items) throws Exception {
			for (String item : items) {
				written.add(item);
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

		private List<String> processed = new ArrayList<>();

		private JdbcTemplate jdbcTemplate;

		/**
		 * @param dataSource
		 */
		public SkipProcessorStub(DataSource dataSource) {
			jdbcTemplate = new JdbcTemplate(dataSource);
		}

		public List<String> getCommitted() {
			return jdbcTemplate.query("SELECT MESSAGE from ERROR_LOG where STEP_NAME='processed'",
					new RowMapper<String>() {
						@Override
						public String mapRow(ResultSet rs, int rowNum) throws SQLException {
							return rs.getString(1);
						}
					});
		}

		public void clear() {
			processed.clear();
			jdbcTemplate.update("DELETE FROM ERROR_LOG where STEP_NAME='processed'");
		}

		@Nullable
		@Override
		public String process(String item) throws Exception {
			processed.add(item);
			logger.debug("Processed item: "+item);
			jdbcTemplate.update("INSERT INTO ERROR_LOG (MESSAGE, STEP_NAME) VALUES (?, ?)", item, "processed");
			return item;
		}
	}

}
