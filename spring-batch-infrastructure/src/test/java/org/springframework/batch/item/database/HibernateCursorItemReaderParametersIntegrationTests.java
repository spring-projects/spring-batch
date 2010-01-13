package org.springframework.batch.item.database;

import java.util.Collections;

import org.hibernate.StatelessSession;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class HibernateCursorItemReaderParametersIntegrationTests extends
		AbstractHibernateCursorItemReaderIntegrationTests {

	protected void setQuery(HibernateCursorItemReader<?> reader) {
		reader.setQueryString("from Foo where name like :name");
		reader.setParameterValues(Collections.singletonMap("name", (Object) "bar%"));
	}

}
