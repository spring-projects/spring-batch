package org.springframework.batch.sample.item.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.batch.sample.retry.RetrySampleItemWriter;

/**
 * Tests for {@link RetrySampleItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class RetrySampleItemWriterTests {

	private RetrySampleItemWriter<Object> processor = new RetrySampleItemWriter<Object>();

	/**
	 * Processing throws exception on 2nd and 3rd call.
	 */
	@Test
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
