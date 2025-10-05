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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * @author Dave Syer
 *
 */
class SingleItemPeekableItemReaderTests {

	/**
	 * Test method for {@link SingleItemPeekableItemReader#read()}.
	 */
	@Test
	void testRead() throws Exception {
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(
				new CountingListItemReader<>(Arrays.asList("a", "b")));
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertNull(reader.read());
	}

	/**
	 * Test method for {@link SingleItemPeekableItemReader#peek()}.
	 */
	@Test
	void testPeek() throws Exception {
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(
				new CountingListItemReader<>(Arrays.asList("a", "b")));
		assertEquals("a", reader.peek());
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertNull(reader.peek());
		assertNull(reader.read());
	}

	/**
	 * Test method for {@link SingleItemPeekableItemReader#close()}.
	 */
	@Test
	void testCloseAndOpenNoPeek() throws Exception {
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(
				new CountingListItemReader<>(Arrays.asList("a", "b")));
		assertEquals("a", reader.read());
		ExecutionContext executionContext = new ExecutionContext();
		reader.update(executionContext);
		reader.close();
		reader.open(executionContext);
		assertEquals("b", reader.read());
	}

	/**
	 * Test method for {@link SingleItemPeekableItemReader#close()}.
	 */
	@Test
	void testCloseAndOpenWithPeek() throws Exception {
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(
				new CountingListItemReader<>(Arrays.asList("a", "b", "c")));
		assertEquals("a", reader.read());
		assertEquals("b", reader.peek());
		ExecutionContext executionContext = new ExecutionContext();
		reader.update(executionContext);
		reader.close();
		reader.open(executionContext);
		assertEquals("b", reader.read());
	}

	@Test
	void testCloseAndOpenWithPeekAndRead() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(
				new CountingListItemReader<>(Arrays.asList("a", "b", "c")));
		assertEquals("a", reader.read());
		assertEquals("b", reader.peek());
		reader.update(executionContext);
		reader.close();
		reader.open(executionContext);
		assertEquals("b", reader.read());
		assertEquals("c", reader.peek());
		reader.update(executionContext);
		reader.close();
		reader.open(executionContext);
		assertEquals("c", reader.read());
	}

	static class CountingListItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

		private final List<T> list;

		private int counter = 0;

		public CountingListItemReader(List<T> list) {
			this.list = list;
			setName("foo");
		}

		@Override
		protected void doClose() throws Exception {
			counter = 0;
		}

		@Override
		protected void doOpen() throws Exception {
			counter = 0;
		}

		@Override
		protected @Nullable T doRead() throws Exception {
			if (counter >= list.size()) {
				return null;
			}
			return list.get(counter++);
		}

	}

}
