/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.support.IteratorItemReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IteratorItemReaderTests {

	@Test
	void testIterable() {
		IteratorItemReader<String> reader = new IteratorItemReader<>(List.of("a", "b", "c"));
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertEquals("c", reader.read());
		assertNull(reader.read());
	}

	@Test
	void testIterator() {
		IteratorItemReader<String> reader = new IteratorItemReader<>(List.of("a", "b", "c").iterator());
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertEquals("c", reader.read());
		assertNull(reader.read());
	}

}
