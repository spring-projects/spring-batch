package org.springframework.batch.core.repository.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

public abstract class AbstractJobExecutionDaoTests extends AbstractTransactionalDataSourceSpringContextTests {

	JobExecutionDao dao;

	JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), "execTestJob");

	JobExecution execution = new JobExecution(jobInstance);

	/**
	 * @return tested object ready for use
	 */
	protected abstract JobExecutionDao getJobExecutionDao();

	protected void onSetUp() throws Exception {
		dao = getJobExecutionDao();
	}

	/**
	 * Save and find a job execution.
	 */
	public void testSaveAndFind() {

		dao.saveJobExecution(execution);

		List<JobExecution> executions = dao.findJobExecutions(jobInstance);
		assertTrue(executions.size() == 1);
		assertEquals(execution, executions.get(0));
	}

	/**
	 * Saving sets id to the entity.
	 */
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
		exec1.setCreateTime(new Date(0));

		ExecutionContext ctx = new ExecutionContext() {
			{
				put("key", "value");
			}
		};
		JobExecution exec2 = new JobExecution(jobInstance);
		exec2.setExecutionContext(ctx);
		exec2.setCreateTime(new Date(1));

		dao.saveJobExecution(exec1);
		dao.saveJobExecution(exec2);
		dao.persistExecutionContext(exec2);

		JobExecution last = dao.getLastJobExecution(jobInstance);
		assertEquals(exec2, last);
		assertEquals("value", last.getExecutionContext().getString("key"));
	}

	public void testSaveAndFindContext() {
		dao.saveJobExecution(execution);
		ExecutionContext ctx = new ExecutionContext(new HashMap<String, Object>() {
			{
				put("key", "value");
			}
		});
		execution.setExecutionContext(ctx);
		dao.persistExecutionContext(execution);

		ExecutionContext retrieved = dao.findExecutionContext(execution);
		assertEquals(ctx, retrieved);
	}

	public void testSaveAndFindEmptyContext() {
		dao.saveJobExecution(execution);
		ExecutionContext ctx = new ExecutionContext();
		execution.setExecutionContext(ctx);
		dao.persistExecutionContext(execution);

		ExecutionContext retrieved = dao.findExecutionContext(execution);
		assertEquals(ctx, retrieved);
	}

	public void testUpdateContext() {
		dao.saveJobExecution(execution);
		ExecutionContext ctx = new ExecutionContext(new HashMap<String, Object>() {
			{
				put("key", "value");
			}
		});
		execution.setExecutionContext(ctx);
		dao.persistExecutionContext(execution);

		ctx.putLong("longKey", 7);
		dao.persistExecutionContext(execution);

		ExecutionContext retrieved = dao.findExecutionContext(execution);
		assertEquals(ctx, retrieved);
		assertEquals(7, retrieved.getLong("longKey"));
	}
}
