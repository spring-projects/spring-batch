package org.springframework.batch.core.repository.dao;


import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "sql-dao-test.xml" })
public class JdbcJobExecutionDaoTests extends AbstractJobExecutionDaoTests {

	@Autowired
	private StepExecutionDao stepExecutionDao;

	@Autowired
	private JobExecutionDao jobExecutionDao;

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		deleteFromTables("BATCH_JOB_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION", "BATCH_JOB_PARAMS",
				"BATCH_JOB_INSTANCE");

		// job instance needs to exist before job execution can be created
		simpleJdbcTemplate
				.getJdbcOperations()
				.execute(
						"insert into BATCH_JOB_INSTANCE (JOB_INSTANCE_ID, JOB_NAME, JOB_KEY, VERSION) values (1,'execTestJob', '', 0)");
		return jobExecutionDao;
	}

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		return stepExecutionDao;
	}
	
}
