package org.springframework.batch.execution.repository.dao;

import java.util.List;
import java.util.Map;

import org.springframework.batch.repeat.ExitStatus;

public class JdbcJobDaoTests extends AbstractJobDaoTests {

	public static final String LONG_STRING = "A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String A very long String ";

	protected void onSetUpBeforeTransaction() throws Exception {
		((JdbcJobInstanceDao) jobInstanceDao).setTablePrefix(AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX);
		((JdbcJobExecutionDao) jobExecutionDao).setTablePrefix(AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX);
	}

	public void testUpdateJobExecutionWithLongExitCode() {

		assertTrue(LONG_STRING.length() > 250);
		jobExecution.setExitStatus(ExitStatus.FINISHED
				.addExitDescription(LONG_STRING));
		jobExecutionDao.updateJobExecution(jobExecution);

		List executions = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_EXECUTION where JOB_INSTANCE_ID=?",
				new Object[] { jobInstance.getId() });
		assertEquals(1, executions.size());
		assertEquals(LONG_STRING.substring(0, 250), ((Map) executions.get(0))
				.get("EXIT_MESSAGE"));
	}

}
