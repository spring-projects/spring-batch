package org.springframework.batch.item.database;

import static org.junit.Assert.fail;

import org.hibernate.StatelessSession;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 * 
 * @author Robert Kasanicky
 */
public class HibernateCursorItemReaderIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {

	/**
	 * Exception scenario.
	 * 
	 * {@link HibernateCursorItemReader#setUseStatelessSession(boolean)} can be
	 * called only in uninitialized state.
	 */
	@Test
	public void testSetUseStatelessSession() {
		HibernateCursorItemReader<Foo> inputSource = (HibernateCursorItemReader<Foo>)reader;

		// initialize and call setter => error
		inputSource.open(new ExecutionContext());
		try {
			inputSource.setUseStatelessSession(false);
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

}
