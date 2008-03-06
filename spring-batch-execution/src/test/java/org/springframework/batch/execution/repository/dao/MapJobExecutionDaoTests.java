package org.springframework.batch.execution.repository.dao;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.execution.job.JobSupport;

public class MapJobExecutionDaoTests extends TestCase {

	JobExecutionDao dao = new MapJobExecutionDao();

	JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("execTestJob"));
	
	JobExecution execution = new JobExecution(jobInstance);

	protected void setUp() throws Exception {
		MapJobExecutionDao.clear();
	}

	/**
	 * Save and find a job execution.
	 */
	public void testSaveAndFind() {
		
		dao.saveJobExecution(execution);

		List executions = dao.findJobExecutions(jobInstance);
		assertTrue(executions.size() == 1);
		assertEquals(execution, executions.get(0));
	}

	/**
	 * Saving sets id to the entity.
	 */
	public void testSaveAddsId() {
		
		assertNull(execution.getId());
		dao.saveJobExecution(execution);
		assertNotNull(execution.getId());
	}

	/**
	 * Execution count increases by one with every save for the same job
	 * instance.
	 */
	public void testGetExecutionCount() {
		
		JobExecution exec1 = new JobExecution(jobInstance);
		JobExecution exec2 = new JobExecution(jobInstance);

		dao.saveJobExecution(exec1);
		assertEquals(1, dao.getJobExecutionCount(jobInstance));

		dao.saveJobExecution(exec2);
		assertEquals(2, dao.getJobExecutionCount(jobInstance));
	}

	/**
	 * Update and retrieve job execution - check attributes have changed as
	 * expected.
	 */
	public void testUpdateExecution() {
		execution.setStatus(BatchStatus.STARTED);
		dao.saveJobExecution(execution);
		
		execution.setStatus(BatchStatus.COMPLETED);
		dao.updateJobExecution(execution);
		
		JobExecution updated = (JobExecution) dao.findJobExecutions(jobInstance).get(0);
		assertEquals(execution, updated);
		assertEquals(BatchStatus.COMPLETED, updated.getStatus());
	}
	
	/**
	 * Check the execution with most recent start time is returned
	 */
	public void testGetLastExecution() {
		JobExecution exec1 = new JobExecution(jobInstance);
		exec1.setStartTime(new Date(0));
		JobExecution exec2 = new JobExecution(jobInstance);
		exec2.setStartTime(new Date(1));
		
		dao.saveJobExecution(exec1);
		dao.saveJobExecution(exec2);
		
		assertEquals(exec2, dao.getLastJobExecution(jobInstance));
	}
}
