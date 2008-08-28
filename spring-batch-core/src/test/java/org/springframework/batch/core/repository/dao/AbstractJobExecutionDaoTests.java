package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractJobExecutionDaoTests extends AbstractTransactionalJUnit4SpringContextTests {

	JobExecutionDao dao;

	JobInstance jobInstance = new JobInstance((long) 1, new JobParameters(), "execTestJob");

	JobExecution execution = new JobExecution(jobInstance);

	/**
	 * @return tested object ready for use
	 */
	protected abstract JobExecutionDao getJobExecutionDao();

	/**
	 * @return tested object ready for use
	 */
	protected StepExecutionDao getStepExecutionDao() {
		return null;
	}

	@Before
	public void onSetUp() throws Exception {
		dao = getJobExecutionDao();
	}

	/**
	 * Save and find a job execution.
	 */
	@Transactional
	@Test
	public void testSaveAndFind() {

		execution.setStartTime(new Date(System.currentTimeMillis()));
		execution.setLastUpdated(new Date(System.currentTimeMillis()));
		dao.saveJobExecution(execution);

		List<JobExecution> executions = dao.findJobExecutions(jobInstance);
		assertEquals(1, executions.size());
		assertEquals(execution, executions.get(0));
		assertExecutionsAreEqual(execution, executions.get(0));
	}

	/**
	 * Save and find a job execution.
	 */
	@Transactional
	@Test
	public void testFindNonExistentExecutions() {
		List<JobExecution> executions = dao.findJobExecutions(jobInstance);
		assertEquals(0, executions.size());
	}

	/**
	 * Saving sets id to the entity.
	 */
	@Transactional
	@Test
	public void testSaveAddsIdAndVersion() {

		assertNull(execution.getId());
		assertNull(execution.getVersion());
		dao.saveJobExecution(execution);
		assertNotNull(execution.getId());
		assertNotNull(execution.getVersion());
	}

	/**
	 * Update and retrieve job execution - check attributes have changed as
	 * expected.
	 */
	@Transactional
	@Test
	public void testUpdateExecution() {
		execution.setStatus(BatchStatus.STARTED);
		dao.saveJobExecution(execution);

		execution.setLastUpdated(new Date(0));
		execution.setStatus(BatchStatus.COMPLETED);
		dao.updateJobExecution(execution);

		JobExecution updated = dao.findJobExecutions(jobInstance).get(0);
		assertEquals(execution, updated);
		assertEquals(BatchStatus.COMPLETED, updated.getStatus());
		assertExecutionsAreEqual(execution, updated);
	}

	/**
	 * Check the execution with most recent start time is returned
	 */
	@Transactional
	@Test
	public void testGetLastExecution() {
		JobExecution exec1 = new JobExecution(jobInstance);
		exec1.setCreateTime(new Date(0));

		JobExecution exec2 = new JobExecution(jobInstance);
		exec2.setCreateTime(new Date(1));

		dao.saveJobExecution(exec1);
		dao.saveJobExecution(exec2);

		JobExecution last = dao.getLastJobExecution(jobInstance);
		assertEquals(exec2, last);
	}

	/**
	 * Check the execution is returned
	 */
	@Transactional
	@Test
	public void testGetMissingLastExecution() {
		JobExecution value = dao.getLastJobExecution(jobInstance);
		assertNull(value);
	}
	
	/**
	 * Check the execution is returned
	 */
	@Transactional
	@Test
	public void testFindRunningExecutions() {
		JobExecution exec = new JobExecution(jobInstance);
		exec.setCreateTime(new Date(0));
		exec.setEndTime(new Date(1L));
		exec.setLastUpdated(new Date(5L));
		dao.saveJobExecution(exec);
		exec = new JobExecution(jobInstance);
		exec.setLastUpdated(new Date(5L));
		exec.createStepExecution(new StepSupport("foo"));
		dao.saveJobExecution(exec);
		StepExecutionDao stepExecutionDao = getStepExecutionDao();
		if (stepExecutionDao != null) {
			for (StepExecution stepExecution : exec.getStepExecutions()) {
				stepExecutionDao.saveStepExecution(stepExecution);
			}
		}
		Set<JobExecution> values = dao.findRunningJobExecutions(exec.getJobInstance().getJobName());

		assertEquals(1, values.size());
		JobExecution value = values.iterator().next();
		assertEquals(exec, value);
		assertEquals(5L,  value.getLastUpdated().getTime());
	}

	/**
	 * Check the execution is returned
	 */
	@Transactional
	@Test
	public void testNoRunningExecutions() {
		Set<JobExecution> values = dao.findRunningJobExecutions("no-such-job");
		assertEquals(0, values.size());
	}
	
	/**
	 * Check the execution is returned
	 */
	@Transactional
	@Test
	public void testGetExecution() {
		JobExecution exec = new JobExecution(jobInstance);
		exec.setCreateTime(new Date(0));
		exec.createStepExecution(new StepSupport("foo"));

		dao.saveJobExecution(exec);
		StepExecutionDao stepExecutionDao = getStepExecutionDao();
		if (stepExecutionDao != null) {
			for (StepExecution stepExecution : exec.getStepExecutions()) {
				stepExecutionDao.saveStepExecution(stepExecution);
			}
		}
		JobExecution value = dao.getJobExecution(exec.getId());

		assertEquals(exec, value);
	}

	/**
	 * Check the execution is returned
	 */
	@Transactional
	@Test
	public void testGetMissingExecution() {
		JobExecution value = dao.getJobExecution(54321L);
		assertNull(value);
	}
	
	/*
	 * Check to make sure the executions are equal.  Normally, comparing the id's is 
	 * sufficient.  However, for testing purposes, especially of a dao, we need to make
	 * sure all the fields are being stored/retrieved correctly.
	 */
	private void assertExecutionsAreEqual(JobExecution lhs, JobExecution rhs){
		
		assertEquals(lhs.getId(), rhs.getId());
		assertEquals(lhs.getStartTime(), rhs.getStartTime());
		assertEquals(lhs.getStatus(), rhs.getStatus());
		assertEquals(lhs.getEndTime(), rhs.getEndTime());
		assertEquals(lhs.getCreateTime(), rhs.getCreateTime());
		assertEquals(lhs.getLastUpdated(), rhs.getLastUpdated());
	}
	
}
