package org.springframework.batch.core.repository.dao;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.repeat.ExitStatus;

public class JdbcStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	protected StepExecutionDao getStepExecutionDao() {
		return (StepExecutionDao) getApplicationContext().getBean("stepExecutionDao");
	}

	protected JobRepository getJobRepository() {
		deleteFromTables(new String[] { "BATCH_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION",
				"BATCH_JOB_PARAMS", "BATCH_JOB_INSTANCE" });
		return (JobRepository) getApplicationContext().getBean("jobRepository");
	}

	protected String[] getConfigLocations() {
		return new String[] { "sql-dao-test.xml" };
	}

	/**
	 * Long exit descriptions are truncated on both save and update.
	 */
	public void testTruncateExitDescription() {
		
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 100; i++) {
			sb.append("too long exit description");
		}
		String longDescription = sb.toString();
		
		ExitStatus exitStatus = ExitStatus.FAILED.addExitDescription(longDescription);
		
		stepExecution.setExitStatus(exitStatus);

		dao.saveStepExecution(stepExecution);

		StepExecution retrievedAfterSave = dao.getStepExecution(jobExecution, step);

		assertTrue("Exit description should be truncated", retrievedAfterSave.getExitStatus().getExitDescription()
				.length() < stepExecution.getExitStatus().getExitDescription().length());

		dao.updateStepExecution(stepExecution);

		StepExecution retrievedAfterUpdate = dao.getStepExecution(jobExecution, step);

		assertTrue("Exit description should be truncated", retrievedAfterUpdate.getExitStatus().getExitDescription()
				.length() < stepExecution.getExitStatus().getExitDescription().length());
	}
}
