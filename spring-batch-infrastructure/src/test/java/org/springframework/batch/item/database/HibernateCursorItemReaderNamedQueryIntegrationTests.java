package org.springframework.batch.item.database;

/**
 * Tests {@link HibernateCursorItemReader} configured with named query.
 */
public class HibernateCursorItemReaderNamedQueryIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {

	@Override
	protected void setQuery(HibernateCursorItemReader<?> reader) {
		reader.setQueryName("allFoos");
	}

}
