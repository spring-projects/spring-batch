package org.springframework.batch.execution.repository.dao;

import java.util.List;
import java.util.Map;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.dao.DataAccessException;

public class SqlStepDaoTests extends AbstractStepDaoTests {

	private static final String LONG_STRING = SqlJobDaoTests.LONG_STRING;

	protected void onSetUpBeforeTransaction() throws Exception {
		((SqlStepDao) stepDao).setTablePrefix(SqlJobDao.DEFAULT_TABLE_PREFIX);
	}

	public void testTablePrefix() throws Exception {
		((SqlStepDao) stepDao).setTablePrefix("FOO_");
		try {
			testCreateStep();
			fail("Expected DataAccessException");
		} catch (DataAccessException e) {
			// expected
		}
	}
	
	public void testUpdateStepExecutionWithLongExitCode() {

		assertTrue(LONG_STRING.length()>250);
		stepExecution.setExitStatus(ExitStatus.FINISHED.addExitDescription(LONG_STRING));
		stepDao.update(stepExecution);

		List executions = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_STEP_EXECUTION where STEP_ID=?",
				new Object[] { step1.getId() });
		assertEquals(1, executions.size());
		assertEquals(LONG_STRING.substring(0, 250), ((Map) executions.get(0))
				.get("EXIT_MESSAGE"));
	}
	
}
