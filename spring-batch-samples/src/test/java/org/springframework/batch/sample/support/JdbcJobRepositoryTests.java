/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.sample.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml" })
public class JdbcJobRepositoryTests {

	private JobRepository repository;

	private JobSupport job;

	private Set<Long> jobExecutionIds = new HashSet<Long>();

	private Set<Long> jobIds = new HashSet<Long>();

	private List<Serializable> list = new ArrayList<Serializable>();

	private SimpleJdbcTemplate simpleJdbcTemplate;

	private PlatformTransactionManager transactionManager;

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Autowired
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Autowired
	public void setRepository(JobRepository repository) {
		this.repository = repository;
	}

	@BeforeTransaction
	public void onSetUpInTransaction() throws Exception {
		job = new JobSupport("test-job");
		job.setRestartable(true);
		simpleJdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT");
		simpleJdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT");
		simpleJdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION");
		simpleJdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION");
		simpleJdbcTemplate.update("DELETE FROM BATCH_JOB_PARAMS");
		simpleJdbcTemplate.update("DELETE FROM BATCH_JOB_INSTANCE");
	}

	@AfterTransaction
	public void onTearDownAfterTransaction() throws Exception {
		for (Long id : jobExecutionIds) {
			simpleJdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT where JOB_EXECUTION_ID=?", id);
			simpleJdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION where JOB_EXECUTION_ID=?", id);
		}
		for (Long id : jobIds) {
			simpleJdbcTemplate.update("DELETE FROM BATCH_JOB_INSTANCE where JOB_INSTANCE_ID=?", id);
		}
		for (Long id : jobIds) {
			int count = simpleJdbcTemplate.queryForInt(
					"SELECT COUNT(*) FROM BATCH_JOB_INSTANCE where JOB_INSTANCE_ID=?", id);
			assertEquals(0, count);
		}
	}

	@Transactional
	@Test
	public void testFindOrCreateJob() throws Exception {
		job.setName("foo");
		int before = 0;
		JobExecution execution = repository.createJobExecution(job.getName(), new JobParameters());
		int after = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE");
		assertEquals(before + 1, after);
		assertNotNull(execution.getId());
	}

	@Transactional
	@Test
	public void testFindOrCreateJobConcurrently() throws Exception {

		job.setName("bar");

		int before = 0;
		assertEquals(0, before);

		long t0 = System.currentTimeMillis();
		try {
			doConcurrentStart();
			fail("Expected JobExecutionAlreadyRunningException");
		}
		catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
		long t1 = System.currentTimeMillis();

		JobExecution execution = (JobExecution) list.get(0);

		assertNotNull(execution);

		int after = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE");
		assertNotNull(execution.getId());
		assertEquals(before + 1, after);

		logger.info("Duration: " + (t1 - t0)
				+ " - the second transaction did not block if this number is less than about 1000.");
	}

	@Test
	public void testFindOrCreateJobConcurrentlyWhenJobAlreadyExists() throws Exception {

		job = new JobSupport("test-job");
		job.setRestartable(true);
		job.setName("spam");

		JobExecution execution = repository.createJobExecution(job.getName(), new JobParameters());
		cacheJobIds(execution);
		execution.setEndTime(new Timestamp(System.currentTimeMillis()));
		repository.update(execution);
		execution.setStatus(BatchStatus.FAILED);

		int before = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE");
		assertEquals(1, before);

		long t0 = System.currentTimeMillis();
		try {
			doConcurrentStart();
			fail("Expected JobExecutionAlreadyRunningException");
		}
		catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
		long t1 = System.currentTimeMillis();

		int after = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE");
		assertNotNull(execution.getId());
		assertEquals(before, after);

		logger.info("Duration: " + (t1 - t0)
				+ " - the second transaction did not block if this number is less than about 1000.");
	}

	private void cacheJobIds(JobExecution execution) {
		if (execution == null)
			return;
		jobExecutionIds.add(execution.getId());
		jobIds.add(execution.getJobId());
	}

	private JobExecution doConcurrentStart() throws Exception {
		new Thread(new Runnable() {
			public void run() {
				try {
					new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
						public Object doInTransaction(org.springframework.transaction.TransactionStatus status) {
							try {
								JobExecution execution = repository.createJobExecution(job.getName(),
										new JobParameters());
								cacheJobIds(execution);
								list.add(execution);
								Thread.sleep(1000);
							}
							catch (Exception e) {
								list.add(e);
							}
							return null;
						}
					});
				}
				catch (RuntimeException e) {
					list.add(e);
				}

			}
		}).start();

		Thread.sleep(400);
		JobExecution execution = repository.createJobExecution(job.getName(), new JobParameters());
		cacheJobIds(execution);

		int count = 0;
		while (list.size() == 0 && count++ < 100) {
			Thread.sleep(200);
		}

		assertEquals("Timed out waiting for JobExecution to be created", 1, list.size());
		assertTrue("JobExecution not created in thread", list.get(0) instanceof JobExecution);
		return (JobExecution) list.get(0);
	}

}
