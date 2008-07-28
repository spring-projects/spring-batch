package org.springframework.batch.item.support;

import java.util.Collection;

import junit.framework.TestCase;

import org.springframework.batch.item.ItemReader;

public class AggregateItemReaderTests extends TestCase {

	private ItemReader<Object> input;

	private AggregateItemReader provider;

	public void setUp() {
		// create mock for input
		input = new AbstractItemReader<Object>() {

			private int count = 0;

			public Object read() {
				switch (count++) {
				case 0:
					return AggregateItemReader.BEGIN_RECORD;
				case 1:
				case 2:
				case 3:
					return "line";
				case 4:
					return AggregateItemReader.END_RECORD;
				default:
					return null;
				}
			}

		};
		// create provider
		provider = new AggregateItemReader();
		provider.setItemReader(input);
	}

	public void testNext() throws Exception {
		// read object
		Object result = provider.read();

		// it should be collection of 3 strings "line"
		assertTrue(result instanceof Collection);
		Collection<?> lines = (Collection<?>) result;
		assertEquals(3, lines.size());

		for (Object line : lines) {
			assertEquals("line", line);
		}

		// read object again - it should return null
		assertNull(provider.read());

	}

}
