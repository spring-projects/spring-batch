package org.springframework.batch.execution.repository.dao;

import org.springframework.dao.DataAccessException;


public class SqlJobDaoTests extends AbstractJobDaoTests {

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
	
}
