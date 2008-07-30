package org.springframework.batch.core.repository.dao;

import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

/**
 * Tests for {@link MapExecutionContextDao}.
 */
@RunWith(JUnit4ClassRunner.class)
public class MapExecutionContextDaoTests extends AbstractExecutionContextDaoTests {

	@Override
	protected ExecutionContextDao getExecutionContextDao() {
		return new MapExecutionContextDao();
	}

}

