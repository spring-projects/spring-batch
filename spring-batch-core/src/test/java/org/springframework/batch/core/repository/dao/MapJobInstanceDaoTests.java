package org.springframework.batch.core.repository.dao;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MapJobInstanceDaoTests extends AbstractJobInstanceDaoTests {

	protected JobInstanceDao getJobInstanceDao() {
		return new MapJobInstanceDao();
	}

}
