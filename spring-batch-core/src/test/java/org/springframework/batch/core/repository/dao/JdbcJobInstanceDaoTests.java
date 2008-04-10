package org.springframework.batch.core.repository.dao;


public class JdbcJobInstanceDaoTests extends AbstractJobInstanceDaoTests {
	
	protected JobInstanceDao getJobInstanceDao() {
		deleteFromTables(new String[] { "BATCH_STEP_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION",
				"BATCH_JOB_PARAMS", "BATCH_JOB_INSTANCE" });
		return (JobInstanceDao) getApplicationContext().getBean("jobInstanceDao");
	}
	
	protected String[] getConfigLocations() {
		return new String[] { "sql-dao-test.xml" };
	}

}
