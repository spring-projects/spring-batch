package org.springframework.batch.core.repository.dao;

/**
 * Tests for {@link MapExecutionContextDao}.
 */
public class MapExecutionContextDaoTests extends AbstractExecutionContextDaoTests {

	@Override
	protected ExecutionContextDao getExecutionContextDao() {
		return new MapExecutionContextDao();
	}

}

