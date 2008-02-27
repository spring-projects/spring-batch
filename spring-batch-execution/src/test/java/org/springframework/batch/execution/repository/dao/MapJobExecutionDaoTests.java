package org.springframework.batch.execution.repository.dao;

import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.execution.job.JobSupport;

public class MapJobExecutionDaoTests extends TestCase {

	JobExecutionDao dao = new MapJobExecutionDao();

	JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("execTestJob"));

	protected void setUp() throws Exception {
		MapJobExecutionDao.clear();
	}

	/**
	 * Save and find a job execution.
	 */
	public void testSaveAndFind() {
		JobExecution exec = new JobExecution(jobInstance);
		dao.saveJobExecution(exec);

		List executions = dao.findJobExecutions(jobInstance);
		assertTrue(executions.size() == 1);
		assertEquals(exec, executions.get(0));
	}
	
	/**
	 * Saving sets id to the entity.
	 */
	public void testSaveAddsId() {
		JobExecution exec = new JobExecution(jobInstance);
		assertNull(exec.getId());
		dao.saveJobExecution(exec);
		assertNotNull(exec.getId());
	}
	/**
	 * Execution count increases by one with every save
	 * for the same job instance.
	 */
	public void testGetExecutionCount() {
		JobExecution exec1 = new JobExecution(jobInstance);
		JobExecution exec2 = new JobExecution(jobInstance);
		
		dao.saveJobExecution(exec1);
		assertEquals(1, dao.getJobExecutionCount(jobInstance));
		
		dao.saveJobExecution(exec2);
		assertEquals(2, dao.getJobExecutionCount(jobInstance));
	}
}
