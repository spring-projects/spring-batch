package org.springframework.batch.core.repository.dao;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"sql-dao-test.xml"})
public class JdbcExecutionContextDaoTests extends AbstractExecutionContextDaoTests {

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		return applicationContext.getBean("jobInstanceDao", JobInstanceDao.class);
	}

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		return applicationContext.getBean("jobExecutionDao", JdbcJobExecutionDao.class);
	}

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		return applicationContext.getBean("stepExecutionDao", StepExecutionDao.class);
	}

	@Override
	protected ExecutionContextDao getExecutionContextDao() {
		return applicationContext.getBean("executionContextDao", JdbcExecutionContextDao.class);
	}

}
