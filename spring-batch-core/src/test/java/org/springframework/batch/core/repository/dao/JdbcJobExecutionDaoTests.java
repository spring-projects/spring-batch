package org.springframework.batch.core.repository.dao;

public class JdbcJobExecutionDaoTests extends AbstractJobExecutionDaoTests {

	protected JobExecutionDao getJobExecutionDao() {
		deleteFromTables(new String[] { "BATCH_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION",
				"BATCH_JOB_PARAMS", "BATCH_JOB_INSTANCE" });

		// job instance needs to exist before job execution can be created
		getJdbcTemplate()
				.execute(
						"insert into BATCH_JOB_INSTANCE (JOB_INSTANCE_ID, JOB_NAME, JOB_KEY, VERSION) values (1,'execTestJob', '', 0)");
		return (JobExecutionDao) getApplicationContext().getBean("jobExecutionDao");
	}

	protected String[] getConfigLocations() {
		return new String[] { "sql-dao-test.xml" };
	}

}
