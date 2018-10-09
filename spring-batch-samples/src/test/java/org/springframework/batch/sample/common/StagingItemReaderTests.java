/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class StagingItemReaderTests {
	private JdbcOperations jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private StagingItemWriter<String> writer;

	@Autowired
	private StagingItemReader<String> reader;

	private Long jobId = 113L;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeTransaction
	public void onSetUpBeforeTransaction() throws Exception {
		StepExecution stepExecution = new StepExecution("stepName", new JobExecution(new JobInstance(jobId,
				"testJob"), new JobParameters()));
		writer.beforeStep(stepExecution);
		writer.write(Arrays.asList("FOO", "BAR", "SPAM", "BUCKET"));
		reader.beforeStep(stepExecution);
	}

	@AfterTransaction
	public void onTearDownAfterTransaction() throws Exception {
		reader.destroy();
		jdbcTemplate.update("DELETE FROM BATCH_STAGING");
	}

	@Transactional
	@Test
	public void testReaderWithProcessorUpdatesProcessIndicator() throws Exception {
		long id = jdbcTemplate.queryForObject("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", Long.class, jobId);
		String before = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.NEW, before);

		ProcessIndicatorItemWrapper<String> wrapper = reader.read();
		String item = wrapper.getItem();
		assertEquals("FOO", item);

		StagingItemProcessor<String> updater = new StagingItemProcessor<>();
		updater.setJdbcTemplate(jdbcTemplate);
		updater.process(wrapper);

		String after = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.DONE, after);
	}

	@Transactional
	@Test
	public void testUpdateProcessIndicatorAfterCommit() throws Exception {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus transactionStatus) {
				try {
					testReaderWithProcessorUpdatesProcessIndicator();
				}
				catch (Exception e) {
					fail("Unexpected Exception: " + e);
				}
				return null;
			}
		});
		long id = jdbcTemplate.queryForObject("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", Long.class, jobId);
		String before = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, id);
		assertEquals(StagingItemWriter.DONE, before);
	}

	@Transactional
	@Test
	public void testReaderRollsBackProcessIndicator() throws Exception {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		final Long idToUse = txTemplate.execute(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				long id = jdbcTemplate.queryForObject("SELECT MIN(ID) from BATCH_STAGING where JOB_ID=?", Long.class, jobId);
				String before = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
						String.class, id);
				assertEquals(StagingItemWriter.NEW, before);

				ProcessIndicatorItemWrapper<String> wrapper = reader.read();
				assertEquals("FOO", wrapper.getItem());

				transactionStatus.setRollbackOnly();

				return id;
			}
		});

		String after = jdbcTemplate.queryForObject("SELECT PROCESSED from BATCH_STAGING where ID=?",
				String.class, idToUse);
		assertEquals(StagingItemWriter.NEW, after);
	}
}
