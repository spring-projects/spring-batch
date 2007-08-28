package org.springframework.batch.execution.repository.dao;

import org.springframework.dao.DataAccessException;

public class SqlStepDaoTests extends BaseStepDaoTests {

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
	
}
