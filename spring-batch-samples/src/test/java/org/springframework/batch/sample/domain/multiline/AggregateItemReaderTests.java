package org.springframework.batch.sample.domain.multiline;

import static org.junit.Assert.*;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemReader;

public class AggregateItemReaderTests {
	private ItemReader<AggregateItem<String>> input;
	private AggregateItemReader<String> provider;

	@Before
	public void setUp() {
		input = new ItemReader<AggregateItem<String>>() {
			private int count = 0;

			@Override
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

		provider = new AggregateItemReader<String>();
		provider.setItemReader(input);
	}

	@Test
	public void testNext() throws Exception {
		Object result = provider.read();

		Collection<?> lines = (Collection<?>) result;
		assertEquals(3, lines.size());

		for (Object line : lines) {
			assertEquals("line", line);
		}

		assertNull(provider.read());
	}
}
