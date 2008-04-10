package org.springframework.batch.core.repository.dao;

public class MapJobInstanceDaoTests extends AbstractJobInstanceDaoTests {

	protected JobInstanceDao getJobInstanceDao() {
		MapJobInstanceDao.clear();
		return new MapJobInstanceDao();
	}

}
