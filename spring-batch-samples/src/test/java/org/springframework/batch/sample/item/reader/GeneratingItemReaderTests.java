package org.springframework.batch.sample.item.reader;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Tests for {@link GeneratingItemReader}.
 * 
 * @author Robert Kasanicky
 */
public class GeneratingItemReaderTests {

	private GeneratingItemReader reader = new GeneratingItemReader();
	
	/**
	 * Generates a given number of not-null records,
	 * consecutive calls return null.
	 */
	@Test
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
