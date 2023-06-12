/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.batch.sample.common;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringJUnitConfig
class StagingItemReaderTests {

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private StagingItemWriter<String> writer;

	@Autowired
	private StagingItemReader<String> reader;

	private final Long jobId = 113L;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeTransaction
	void onSetUpBeforeTransaction() {
		StepExecution stepExecution = new StepExecution("stepName",
				new JobExecution(new JobInstance(jobId, "testJob"), new JobParameters()));
		writer.beforeStep(stepExecution);
		writer.write(Chunk.of("FOO", "BAR", "SPAM", "BUCKET"));
		reader.beforeStep(stepExecution);
	}

	@AfterTransaction
	void onTearDownAfterTransaction() throws Exception {
		reader.destroy();
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "BATCH_STAGING");
	}

	@Transactional
	@Test
	void testReaderWithProcessorUpdatesProcessIndicator() throws Exception {
		long id = jdbcTemplate.queryForObject("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", Long.class, jobId);
		String before = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?", String.class, id);
		assertEquals(StagingItemWriter.NEW, before);

		ProcessIndicatorItemWrapper<String> wrapper = reader.read();
		String item = wrapper.getItem();
		assertEquals("FOO", item);

		StagingItemProcessor<String> updater = new StagingItemProcessor<>();
		updater.setJdbcTemplate(jdbcTemplate);
		updater.process(wrapper);

		String after = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?", String.class, id);
		assertEquals(StagingItemWriter.DONE, after);
	}

	@Transactional
	@Test
	void testUpdateProcessIndicatorAfterCommit() {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
			try {
				testReaderWithProcessorUpdatesProcessIndicator();
			}
			catch (Exception e) {
				fail("Unexpected Exception: " + e);
			}
			return null;
		});
		long id = jdbcTemplate.queryForObject("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", Long.class, jobId);
		String before = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?", String.class, id);
		assertEquals(StagingItemWriter.DONE, before);
	}

	@Transactional
	@Test
	void testReaderRollsBackProcessIndicator() {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		final Long idToUse = txTemplate.execute(transactionStatus -> {

			long id = jdbcTemplate.queryForObject("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", Long.class,
					jobId);
			String before = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?", String.class,
					id);
			assertEquals(StagingItemWriter.NEW, before);

			ProcessIndicatorItemWrapper<String> wrapper = reader.read();
			assertEquals("FOO", wrapper.getItem());

			transactionStatus.setRollbackOnly();

			return id;
		});

		String after = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?", String.class,
				idToUse);
		assertEquals(StagingItemWriter.NEW, after);
	}

}
