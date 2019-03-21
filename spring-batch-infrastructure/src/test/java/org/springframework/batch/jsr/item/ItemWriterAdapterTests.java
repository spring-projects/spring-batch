/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.jsr.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.batch.api.chunk.ItemWriter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemWriterAdapterTests {

	private ItemWriterAdapter<String> adapter;
	@Mock
	private ItemWriter delegate;
	@Mock
	private ExecutionContext executionContext;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adapter = new ItemWriterAdapter<>(delegate);
		adapter.setName("jsrWriter");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new ItemWriterAdapter<>(null);
	}

	@Test
	public void testOpen() throws Exception {
		when(executionContext.get("jsrWriter.writer.checkpoint")).thenReturn("checkpoint");

		adapter.open(executionContext);

		verify(delegate).open("checkpoint");
	}

	@Test(expected=ItemStreamException.class)
	public void testOpenException() throws Exception {
		when(executionContext.get("jsrWriter.writer.checkpoint")).thenReturn("checkpoint");

		doThrow(new Exception("expected")).when(delegate).open("checkpoint");

		adapter.open(executionContext);
	}

	@Test
	public void testUpdate() throws Exception {
		when(delegate.checkpointInfo()).thenReturn("checkpoint");

		adapter.update(executionContext);

		verify(executionContext).put("jsrWriter.writer.checkpoint", "checkpoint");
	}

	@Test(expected=ItemStreamException.class)
	public void testUpdateException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).checkpointInfo();

		adapter.update(executionContext);
	}

	@Test
	public void testClose() throws Exception {
		adapter.close();

		verify(delegate).close();
	}

	@Test(expected=ItemStreamException.class)
	public void testCloseException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).close();

		adapter.close();
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void testWrite() throws Exception {
		List items = new ArrayList();

		items.add("item1");
		items.add("item2");

		adapter.write(items);

		verify(delegate).writeItems(items);
	}

	@Test
	public void testCheckpointChange() throws Exception {
		ItemWriterAdapter<String> adapter = new ItemWriterAdapter<>(new ItemWriter() {

			private CheckpointContainer container = null;

			@Override
			public void open(Serializable checkpoint) throws Exception {
				container = new CheckpointContainer();
			}

			@Override
			public void close() throws Exception {
			}

			@Override
			public void writeItems(List<Object> items) throws Exception {
				container.setCount(container.getCount() + items.size());
			}

			@Override
			public Serializable checkpointInfo() throws Exception {
				return container;
			}
		});

		ExecutionContext context = new ExecutionContext();

		List<String> items = new ArrayList<>();
		items.add("foo");
		items.add("bar");
		items.add("baz");
		adapter.open(context);
		adapter.write(items);
		adapter.update(context);
		adapter.write(items);
		adapter.close();

		CheckpointContainer container = (CheckpointContainer) context.get("ItemWriterAdapterTests.1.writer.checkpoint");
		assertEquals(3, container.getCount());

	}

	public static class CheckpointContainer implements Serializable{
		private static final long serialVersionUID = 1L;

		private int count;

		public CheckpointContainer() {
			count = 0;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public String toString() {
			return "CheckpointContainer has a count of " + count;
		}
	}
}
