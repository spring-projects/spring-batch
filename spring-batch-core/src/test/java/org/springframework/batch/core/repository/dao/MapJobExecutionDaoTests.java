package org.springframework.batch.core.repository.dao;


public class MapJobExecutionDaoTests extends AbstractJobExecutionDaoTests {

	protected JobExecutionDao getJobExecutionDao() {
		MapJobExecutionDao.clear();
		MapJobInstanceDao.clear();
		return new MapJobExecutionDao();
	}

}
