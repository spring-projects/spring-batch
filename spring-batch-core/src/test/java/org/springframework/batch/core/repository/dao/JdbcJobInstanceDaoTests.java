package org.springframework.batch.core.repository.dao;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.runner.RunWith;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "sql-dao-test.xml")
public class JdbcJobInstanceDaoTests extends AbstractJobInstanceDaoTests {
	
	protected JobInstanceDao getJobInstanceDao() {
		deleteFromTables("BATCH_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION",
				"BATCH_JOB_PARAMS", "BATCH_JOB_INSTANCE");
		return (JobInstanceDao) applicationContext.getBean("jobInstanceDao");
	}
	
}
