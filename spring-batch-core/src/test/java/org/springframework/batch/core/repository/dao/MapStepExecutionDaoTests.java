package org.springframework.batch.core.repository.dao;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;

@RunWith(JUnit4ClassRunner.class)
public class MapStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	protected StepExecutionDao getStepExecutionDao() {
		return new MapStepExecutionDao();
	}

	protected JobRepository getJobRepository() {
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
		return new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(), new MapStepExecutionDao(),
				new MapExecutionContextDao());
	}

}
