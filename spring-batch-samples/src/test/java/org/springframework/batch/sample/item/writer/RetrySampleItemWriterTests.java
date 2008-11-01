package org.springframework.batch.sample.item.writer;

import junit.framework.TestCase;

/**
 * Tests for {@link RetrySampleItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class RetrySampleItemWriterTests extends TestCase {

	private RetrySampleItemWriter processor = new RetrySampleItemWriter();

	/**
	 * Processing throws exception on 2nd and 3rd call.
	 */
	public void testProcess() throws Exception {
		Object item = null;
		processor.write(item);

		for (int i = 0; i < 2; i++) {
			try {
				processor.write(item);
				fail();
			}
			catch (RuntimeException e) {
				// expected
			}
		}
		
		processor.write(item);
		
		assertEquals(4, processor.getCounter());
	}
}
