package org.springframework.batch.core.repository.dao;

import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;

@RunWith(JUnit4ClassRunner.class)
public class MapJobInstanceDaoTests extends AbstractJobInstanceDaoTests {

	protected JobInstanceDao getJobInstanceDao() {
		return new MapJobInstanceDao();
	}

}
