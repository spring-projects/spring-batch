/*
 * Copyright 2008-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample.domain.multiline;

import static org.junit.Assert.*;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemReader;
import org.springframework.lang.Nullable;

public class AggregateItemReaderTests {
	private ItemReader<AggregateItem<String>> input;
	private AggregateItemReader<String> provider;

	@Before
	public void setUp() {
		input = new ItemReader<AggregateItem<String>>() {
			private int count = 0;

			@Nullable
			@Override
			public AggregateItem<String> read() {
				switch (count++) {
				case 0:
					return AggregateItem.getHeader();
				case 1:
				case 2:
				case 3:
					return new AggregateItem<>("line");
				case 4:
					return AggregateItem.getFooter();
				default:
					return null;
				}
			}

		};

		provider = new AggregateItemReader<>();
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
