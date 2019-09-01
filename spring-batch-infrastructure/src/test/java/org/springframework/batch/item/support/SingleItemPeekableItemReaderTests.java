/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.item.support;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 *
 */
public class SingleItemPeekableItemReaderTests {
	
	private SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>();
	
	/**
	 * Test method for {@link org.springframework.batch.item.support.SingleItemPeekableItemReader#read()}.
	 */
	@Test
	public void testRead() throws Exception {
		reader.setDelegate(new CountingListItemReader<>(Arrays.asList("a", "b")));
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertEquals(null, reader.read());
	}

	/**
	 * Test method for {@link org.springframework.batch.item.support.SingleItemPeekableItemReader#peek()}.
	 */
	@Test
	public void testPeek() throws Exception {
		reader.setDelegate(new CountingListItemReader<>(Arrays.asList("a", "b")));
		assertEquals("a", reader.peek());
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertEquals(null, reader.peek());
		assertEquals(null, reader.read());
	}

	/**
	 * Test method for {@link org.springframework.batch.item.support.SingleItemPeekableItemReader#close()}.
	 */
	@Test
	public void testCloseAndOpenNoPeek() throws Exception {
		reader.setDelegate(new CountingListItemReader<>(Arrays.asList("a", "b")));
		assertEquals("a", reader.read());
		ExecutionContext executionContext = new ExecutionContext();
		reader.update(executionContext);
		reader.close();
		reader.open(executionContext);
		assertEquals("b", reader.read());		
	}

	/**
	 * Test method for {@link org.springframework.batch.item.support.SingleItemPeekableItemReader#close()}.
	 */
	@Test
	public void testCloseAndOpenWithPeek() throws Exception {
		reader.setDelegate(new CountingListItemReader<>(Arrays.asList("a", "b", "c")));
		assertEquals("a", reader.read());
		assertEquals("b", reader.peek());
		ExecutionContext executionContext = new ExecutionContext();
		reader.update(executionContext);
		reader.close();
		reader.open(executionContext);
		assertEquals("b", reader.read());		
	}

	@Test
	public void testCloseAndOpenWithPeekAndRead() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		reader.setDelegate(new CountingListItemReader<>(Arrays.asList("a", "b", "c")));
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

	public static class CountingListItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {
		
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

		@Nullable
		@Override
		protected T doRead() throws Exception {
			if (counter>=list.size()) {
				return null;
			}
			return list.get(counter++);
		}
		
	}

}
