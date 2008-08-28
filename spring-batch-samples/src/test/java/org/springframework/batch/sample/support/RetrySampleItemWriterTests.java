package org.springframework.batch.sample.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * Tests for {@link RetrySampleItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class RetrySampleItemWriterTests {

	private RetrySampleItemWriter<Object> processor = new RetrySampleItemWriter<Object>();

	/*
	 * Processing throws exception on 2nd and 3rd call.
	 */
	@Test
	public void testProcess() throws Exception {
		Object item = null;
		processor.write(Collections.singletonList(item));

		try {
			processor.write(Arrays.asList(item, item, item));
			fail();
		}
		catch (RuntimeException e) {
			// expected
		}

		processor.write(Collections.singletonList(item));

		assertEquals(5, processor.getCounter());
	}
}
