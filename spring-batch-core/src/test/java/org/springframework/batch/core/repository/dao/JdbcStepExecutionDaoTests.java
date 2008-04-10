package org.springframework.batch.core.repository.dao;

import org.springframework.batch.core.repository.JobRepository;

public class JdbcStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	protected StepExecutionDao getStepExecutionDao() {
		return (StepExecutionDao) getApplicationContext().getBean("stepExecutionDao");
	}

	protected JobRepository getJobRepository() {
		deleteFromTables(new String[] { "BATCH_STEP_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION",
				"BATCH_JOB_PARAMS", "BATCH_JOB_INSTANCE" });
		return (JobRepository) getApplicationContext().getBean("jobRepository");
	}

	protected String[] getConfigLocations() {
		return new String[] { "sql-dao-test.xml" };
	}

}
