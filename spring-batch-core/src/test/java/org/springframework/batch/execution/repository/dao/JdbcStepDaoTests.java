package org.springframework.batch.execution.repository.dao;

import java.util.List;
import java.util.Map;

import org.springframework.batch.repeat.ExitStatus;

public class JdbcStepDaoTests extends AbstractStepDaoTests {

	private static final String LONG_STRING = JdbcJobDaoTests.LONG_STRING;

	protected void onSetUpBeforeTransaction() throws Exception {
		((JdbcStepExecutionDao) stepExecutionDao).setTablePrefix(AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX);
	}

	public void testTablePrefix() throws Exception {
//		((JdbcStepInstanceDao) stepInstanceDao).setTablePrefix("FOO_");
//		((JdbcStepExecutionDao) stepExecutionDao).setTablePrefix("FOO_");
//		try {
//			testCreateStep();
//			fail("Expected DataAccessException");
//		} catch (DataAccessException e) {
//			// expected
//		}
	}
	
	public void testUpdateStepExecutionWithLongExitCode() {

		assertTrue(LONG_STRING.length()>250);
		stepExecution.setExitStatus(ExitStatus.FINISHED.addExitDescription(LONG_STRING));
		stepExecutionDao.updateStepExecution(stepExecution);

		List executions = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_STEP_EXECUTION where STEP_NAME=?",
				new Object[] { step1.getName() });
		assertEquals(1, executions.size());
		assertEquals(LONG_STRING.substring(0, 250), ((Map) executions.get(0))
				.get("EXIT_MESSAGE"));
	}

}
