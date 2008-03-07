package org.springframework.batch.item.database;

import org.hibernate.Session;
import org.springframework.batch.item.database.HibernateCursorItemReader;

/**
 * Tests for {@link HibernateCursorItemReader} using standard hibernate {@link Session}.
 * 
 * @author Robert Kasanicky
 */
public class HibernateCursorItemReaderStatefulIntegrationTests extends HibernateCursorItemReaderIntegrationTests {

	protected boolean isUseStatelessSession() {
		return false;
	}
	
}
