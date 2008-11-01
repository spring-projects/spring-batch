package org.springframework.batch.sample.item.reader;

import junit.framework.TestCase;

/**
 * Tests for {@link GeneratingItemReader}.
 * 
 * @author Robert Kasanicky
 */
public class GeneratingItemReaderTests extends TestCase {

	private GeneratingItemReader reader = new GeneratingItemReader();
	
	/**
	 * Generates a given number of not-null records,
	 * consecutive calls return null.
	 */
	public void testRead() throws Exception {
		int counter = 0;
		int limit = 10;
		reader.setLimit(limit);
		
		while (reader.read() != null) {
			counter++;
		}
		
		assertEquals(null, reader.read());
		assertEquals(limit, counter);
		assertEquals(counter, reader.getCounter());
	}
}
