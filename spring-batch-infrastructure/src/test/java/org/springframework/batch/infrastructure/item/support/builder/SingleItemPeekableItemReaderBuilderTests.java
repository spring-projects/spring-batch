/*
 * Copyright 2017-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.infrastructure.item.support.builder;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Glenn Renfro
 */
class SingleItemPeekableItemReaderBuilderTests {

	/**
	 * Test method to validate builder creates a
	 * {@link SingleItemPeekableItemReader#peek()} with expected peek and read behavior.
	 */
	@Test
	void testPeek() throws Exception {
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReaderBuilder<String>()
			.delegate(new ListItemReader<>(Arrays.asList("a", "b")))
			.build();
		assertEquals("a", reader.peek());
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertNull(reader.peek());
		assertNull(reader.read());
	}

	/**
	 * Test method to validate that an {@link IllegalArgumentException} is thrown if the
	 * delegate is not set in the builder.
	 */
	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new SingleItemPeekableItemReaderBuilder<Foo>().build());
		assertEquals("A delegate is required", exception.getMessage());
	}

}
