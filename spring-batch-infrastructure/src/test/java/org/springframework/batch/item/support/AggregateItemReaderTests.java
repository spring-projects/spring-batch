package org.springframework.batch.item.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemReader;

public class AggregateItemReaderTests {

	private ItemReader<AggregateItem<String>> input;

	private AggregateItemReader<String> provider;

	@Before
	public void setUp() {
		// create mock for input
		input = new ItemReader<AggregateItem<String>>() {

			private int count = 0;

			public AggregateItem<String> read() {
				switch (count++) {
				case 0:
					return AggregateItem.getHeader();
				case 1:
				case 2:
				case 3:
					return new AggregateItem<String>("line");
				case 4:
					return AggregateItem.getFooter();
				default:
					return null;
				}
			}

		};
		// create provider
		provider = new AggregateItemReader<String>();
		provider.setItemReader(input);
	}

	@Test
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
