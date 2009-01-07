package org.springframework.batch.item.database;

/**
 * Tests {@link HibernateCursorItemReader} configured with stateful session and
 * named query.
 */
public class HibernateCursorItemReaderStatefulNamedQueryIntegrationTests extends
		HibernateCursorItemReaderNamedQueryIntegrationTests {

	@Override
	protected boolean isUseStatelessSession() {
		return false;
	}
}
