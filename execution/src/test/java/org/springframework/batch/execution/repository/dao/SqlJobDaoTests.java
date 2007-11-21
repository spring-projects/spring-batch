package org.springframework.batch.execution.repository.dao;

import java.util.List;
import java.util.Map;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.dao.DataAccessException;


public class SqlJobDaoTests extends AbstractJobDaoTests {

	private static final String LONG_STRING = BatchHibernateInterceptorTests.LONG_STRING;

	protected void onSetUpBeforeTransaction() throws Exception {
		((SqlJobDao) jobDao).setTablePrefix(SqlJobDao.DEFAULT_TABLE_PREFIX);
	}

	public void testTablePrefix() throws Exception {
		((SqlJobDao) jobDao).setTablePrefix("FOO_");
		try {
			testUpdateJob();
			fail("Expected DataAccessException");
		} catch (DataAccessException e) {
			// expected
		}
	}
	
	public void testUpdateJobExecutionWithLongExitCode() {

		assertTrue(LONG_STRING.length()>250);
		jobExecution.setExitStatus(ExitStatus.FINISHED.addExitDescription(LONG_STRING));
		jobDao.update(jobExecution);

		List executions = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_EXECUTION where JOB_ID=?",
				new Object[] { job.getId() });
		assertEquals(1, executions.size());
		assertEquals(LONG_STRING.substring(0, 250), ((Map) executions.get(0))
				.get("EXIT_MESSAGE"));
	}

}
