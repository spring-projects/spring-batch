package org.springframework.batch.sample.dao;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class JdbcJobRepositoryTests extends AbstractTransactionalDataSourceSpringContextTests {

	private ScheduledJobIdentifier jobIdentifier;

	private JobRepository repository;

	private JobConfiguration jobConfiguration;

	private Set jobExecutionIds = new HashSet();

	private Set jobIds = new HashSet();

	private List list = new ArrayList();

	public void setRepository(JobRepository repository) {
		this.repository = repository;
	}

	protected String[] getConfigLocations() {
		return new String[] { "simple-container-definition.xml" };
	}

	protected void onSetUpInTransaction() throws Exception {
		jobIdentifier = new ScheduledJobIdentifier("Job1", "TestStream", 
				new SimpleDateFormat("yyyyMMdd").parse("20070505"));
		jobConfiguration = new JobConfiguration("test-job");
		jobConfiguration.setRestartable(true);
	}

	protected void onSetUpBeforeTransaction() throws Exception {
		startNewTransaction();
		getJdbcTemplate().update("DELETE FROM BATCH_JOB_EXECUTION");
		getJdbcTemplate().update("DELETE FROM BATCH_JOB");
		setComplete();
		endTransaction();
	}

	protected void onTearDownAfterTransaction() throws Exception {
		startNewTransaction();
		for (Iterator iterator = jobExecutionIds.iterator(); iterator.hasNext();) {
			Long id = (Long) iterator.next();
			getJdbcTemplate().update("DELETE FROM BATCH_JOB_EXECUTION where ID=?", new Object[] { id });
		}
		for (Iterator iterator = jobIds.iterator(); iterator.hasNext();) {
			Long id = (Long) iterator.next();
			getJdbcTemplate().update("DELETE FROM BATCH_JOB where ID=?", new Object[] { id });
		}
		setComplete();
		endTransaction();
		for (Iterator iterator = jobIds.iterator(); iterator.hasNext();) {
			Long id = (Long) iterator.next();
			int count = getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM BATCH_JOB where ID=?", new Object[] { id });
			assertEquals(0, count);
		}
	}

	public void testFindOrCreateJob() throws Exception {
		jobConfiguration.setName("foo");
		int before = getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM BATCH_JOB");
		JobExecution execution = repository.findOrCreateJob(jobConfiguration, jobIdentifier);
		int after = getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM BATCH_JOB");
		assertEquals(before + 1, after);
		assertNotNull(execution.getId());
	}

	public void testFindOrCreateJobConcurrently() throws Exception {

		jobConfiguration.setName("bar");

		int before = getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM BATCH_JOB");
		assertEquals(0, before);

		endTransaction();
		startNewTransaction();

		JobExecution execution = null;
		long t0 = System.currentTimeMillis();
		try {
			execution = doConcurrentStart();
			fail("Expected JobExecutionAlreadyRunningException");
		}
		catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
		long t1 = System.currentTimeMillis();

		if (execution == null) {
			execution = (JobExecution) list.get(0);
		}

		assertNotNull(execution);

		int after = getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM BATCH_JOB");
		assertNotNull(execution.getId());
		assertEquals(before + 1, after);

		logger.info("Duration: " + (t1 - t0)
				+ " - the second transaction did not block if this number is less than about 1000.");
	}

	public void testFindOrCreateJobConcurrentlyWhenJobAlreadyExists() throws Exception {

		jobConfiguration.setName("spam");

		JobExecution execution = repository.findOrCreateJob(jobConfiguration, jobIdentifier);
		cacheJobIds(execution);
		execution.setEndTime(new Timestamp(System.currentTimeMillis()));
		repository.saveOrUpdate(execution);
		JobInstance job = execution.getJob();
		job.setStatus(BatchStatus.FAILED);
		repository.update(job);
		setComplete();
		endTransaction();

		startNewTransaction();

		int before = getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM BATCH_JOB");
		assertEquals(1, before);

		endTransaction();
		startNewTransaction();

		long t0 = System.currentTimeMillis();
		try {
			execution = doConcurrentStart();
			fail("Expected JobExecutionAlreadyRunningException");
		}
		catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
		long t1 = System.currentTimeMillis();

		int after = getJdbcTemplate().queryForInt("SELECT COUNT(*) FROM BATCH_JOB");
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

	private JobExecution doConcurrentStart() throws InterruptedException, JobExecutionAlreadyRunningException {
		new Thread(new Runnable() {
			public void run() {
				try {
					new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
						public Object doInTransaction(org.springframework.transaction.TransactionStatus status) {
							try {
								JobExecution execution = repository.findOrCreateJob(jobConfiguration, jobIdentifier);
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
		JobExecution execution = repository.findOrCreateJob(jobConfiguration, jobIdentifier);
		cacheJobIds(execution);

		int count = 0;
		while (list.size() == 0 && count++ < 10) {
			Thread.sleep(200);
		}

		assertEquals("Timed out waiting for JobExecution to be created", 1, list.size());
		assertTrue("JobExecution not created in thread", list.get(0) instanceof JobExecution);
		return (JobExecution) list.get(0);
	}

}
