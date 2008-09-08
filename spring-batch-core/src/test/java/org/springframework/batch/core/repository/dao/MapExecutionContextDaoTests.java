package org.springframework.batch.core.repository.dao;

import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

/**
 * Tests for {@link MapExecutionContextDao}.
 */
@RunWith(JUnit4ClassRunner.class)
public class MapExecutionContextDaoTests extends AbstractExecutionContextDaoTests {

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		MapJobInstanceDao.clear();
		return new MapJobInstanceDao();
	}

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		MapJobExecutionDao.clear();
		return new MapJobExecutionDao();
	}

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		MapStepExecutionDao.clear();
		return new MapStepExecutionDao();
	}

	@Override
	protected ExecutionContextDao getExecutionContextDao() {
		return new MapExecutionContextDao();
	}

}

