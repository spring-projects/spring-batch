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

		dao.saveJobExecution(execution);

		List<JobExecution> executions = dao.findJobExecutions(jobInstance);
		assertTrue(executions.size() == 1);
		assertEquals(execution, executions.get(0));
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

		execution.setStatus(BatchStatus.COMPLETED);
		dao.updateJobExecution(execution);

		JobExecution updated = dao.findJobExecutions(jobInstance).get(0);
		assertEquals(execution, updated);
		assertEquals(BatchStatus.COMPLETED, updated.getStatus());
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
	public void testFindRunningExecutions() {
		JobExecution exec = new JobExecution(jobInstance);
		exec.setCreateTime(new Date(0));
		exec.setEndTime(new Date(0));
		dao.saveJobExecution(exec);
		exec = new JobExecution(jobInstance);
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
		assertEquals(1, value.getStepExecutions().size());
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
		JobExecution value = dao.getJobExecution(exec.getId());

		assertEquals(exec, value);
		assertEquals(1, value.getStepExecutions().size());
	}

}
