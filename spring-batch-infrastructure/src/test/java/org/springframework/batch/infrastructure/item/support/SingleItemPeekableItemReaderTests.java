/*
 * Copyright 2006-2026 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.infrastructure.item.support.SingleItemPeekableItemReader;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.core.io.ByteArrayResource;

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

	@Test
	void testRepeatedPeekWithException() throws Exception {
		ItemReader<String> delegate = mock();
		Exception exception = new Exception("read failure");
		when(delegate.read()).thenThrow(exception).thenReturn("a");
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(delegate);
		assertSame(exception, assertThrows(Exception.class, reader::peek));
		assertSame(exception, assertThrows(Exception.class, reader::peek));
		verify(delegate).read();
		assertSame(exception, assertThrows(Exception.class, reader::read));
		assertEquals("a", reader.peek());
		assertEquals("a", reader.read());
		verify(delegate, times(2)).read();
	}

	@Test
	void testUpdateWithPeekException() throws Exception {
		SingleItemPeekableItemReader<String> reader = failingReader();
		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);
		assertEquals("a", reader.read());
		assertThrows(FlatFileParseException.class, reader::peek);
		reader.update(executionContext);
		assertEquals(1, executionContext.getInt("delegate.read.count"));
		reader.close();
		reader.open(executionContext);
		assertThrows(FlatFileParseException.class, reader::read);
		assertEquals("b", reader.read());
		reader.close();
	}

	@Test
	void testUpdateAfterReadingPeekException() throws Exception {
		SingleItemPeekableItemReader<String> reader = failingReader();
		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);
		assertEquals("a", reader.read());
		FlatFileParseException exception = assertThrows(FlatFileParseException.class, reader::peek);
		assertSame(exception, assertThrows(FlatFileParseException.class, reader::read));
		reader.update(executionContext);
		assertEquals(2, executionContext.getInt("delegate.read.count"));
		reader.close();
		reader.open(executionContext);
		assertEquals("b", reader.read());
		reader.close();
	}

	@Test
	void testOpenClearsPeekException() throws Exception {
		ItemReader<String> delegate = mock();
		when(delegate.read()).thenThrow(new Exception("read failure")).thenReturn("a");
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(delegate);
		assertThrows(Exception.class, reader::peek);
		reader.open(new ExecutionContext());
		assertEquals("a", reader.read());
	}

	@Test
	void testCloseClearsPeekException() throws Exception {
		ItemReader<String> delegate = mock();
		when(delegate.read()).thenThrow(new Exception("read failure")).thenReturn("a");
		SingleItemPeekableItemReader<String> reader = new SingleItemPeekableItemReader<>(delegate);
		assertThrows(Exception.class, reader::peek);
		reader.close();
		assertEquals("a", reader.read());
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

	private SingleItemPeekableItemReader<String> failingReader() {
		FlatFileItemReader<String> delegate = new FlatFileItemReader<>(
				new ByteArrayResource("a\nbad\nb\n".getBytes(StandardCharsets.UTF_8)), (line, lineNumber) -> {
					if ("bad".equals(line)) {
						throw new IllegalArgumentException("read failure");
					}
					return line;
				});
		delegate.setName("delegate");
		return new SingleItemPeekableItemReader<>(delegate);
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
