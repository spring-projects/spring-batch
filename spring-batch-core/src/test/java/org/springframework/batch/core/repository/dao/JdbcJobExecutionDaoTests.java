package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.*;

import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

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
	
	@Transactional
	@Test
	public void testUpdateExecutionStatus(){
		
		dao.saveJobExecution(execution);
		execution.setStatus(BatchStatus.COMPLETED);
		dao.synchronizeStatus(execution);
		assertEquals(BatchStatus.STARTING, execution.getStatus());
	}
}
