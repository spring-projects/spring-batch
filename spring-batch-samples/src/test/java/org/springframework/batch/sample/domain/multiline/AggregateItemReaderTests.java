/*
 * Copyright 2008-2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemReader;
import org.springframework.lang.Nullable;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AggregateItemReaderTests {

	private ItemReader<AggregateItem<String>> input;

	private AggregateItemReader<String> provider;

	@BeforeEach
	void setUp() {
		input = new ItemReader<>() {
			private int count = 0;

			@Nullable
			@Override
			public AggregateItem<String> read() {
				return switch (count++) {
					case 0 -> AggregateItem.getHeader();
					case 1, 2, 3 -> new AggregateItem<>("line");
					case 4 -> AggregateItem.getFooter();
					default -> null;
				};
			}

		};

		provider = new AggregateItemReader<>();
		provider.setItemReader(input);
	}

	@Test
	void testNext() throws Exception {
		Object result = provider.read();

		Collection<?> lines = (Collection<?>) result;
		assertEquals(3, lines.size());

		for (Object line : lines) {
			assertEquals("line", line);
		}

		assertNull(provider.read());
	}

}
