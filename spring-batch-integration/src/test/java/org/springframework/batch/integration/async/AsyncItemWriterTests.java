/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.async;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mminella
 */
public class AsyncItemWriterTests {

	private AsyncItemWriter<String> writer;
	private List<String> writtenItems;
	private TaskExecutor taskExecutor;

	@Before
	public void setup() {
		taskExecutor = new SimpleAsyncTaskExecutor();
		writtenItems = new ArrayList<String>();
		writer = new AsyncItemWriter<String>();
	}

	@Test
	public void testRoseyScenario() throws Exception {
		writer.setDelegate(new ListItemWriter(writtenItems));
		List<FutureTask<String>> processedItems = new ArrayList<FutureTask<String>>();

		processedItems.add(new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "foo";
			}
		}));

		processedItems.add(new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "bar";
			}
		}));

		for (FutureTask<String> processedItem : processedItems) {
			taskExecutor.execute(processedItem);
		}

		writer.write(processedItems);

		assertEquals(2, writtenItems.size());
		assertTrue(writtenItems.contains("foo"));
		assertTrue(writtenItems.contains("bar"));
	}

	@Test
	public void testFilteredItem() throws Exception {
		writer.setDelegate(new ListItemWriter(writtenItems));
		List<FutureTask<String>> processedItems = new ArrayList<FutureTask<String>>();

		processedItems.add(new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "foo";
			}
		}));

		processedItems.add(new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return null;
			}
		}));

		for (FutureTask<String> processedItem : processedItems) {
			taskExecutor.execute(processedItem);
		}

		writer.write(processedItems);

		assertEquals(1, writtenItems.size());
		assertTrue(writtenItems.contains("foo"));
	}

	@Test
	public void testStreamDelegate() throws Exception {
		ListItemStreamWriter itemWriter = new ListItemStreamWriter(writtenItems);
		writer.setDelegate(itemWriter);

		List<FutureTask<String>> processedItems = new ArrayList<FutureTask<String>>();

		ExecutionContext executionContext = new ExecutionContext();
		writer.open(executionContext);
		writer.write(processedItems);
		writer.update(executionContext);
		writer.close();

		assertTrue(itemWriter.isOpened);
		assertTrue(itemWriter.isUpdated);
		assertTrue(itemWriter.isClosed);
	}

	@Test
	public void testNonStreamDelegate() throws Exception {
		ListItemWriter itemWriter = new ListItemWriter(writtenItems);
		writer.setDelegate(itemWriter);

		List<FutureTask<String>> processedItems = new ArrayList<FutureTask<String>>();

		ExecutionContext executionContext = new ExecutionContext();
		writer.open(executionContext);
		writer.write(processedItems);
		writer.update(executionContext);
		writer.close();

		assertFalse(itemWriter.isOpened);
		assertFalse(itemWriter.isUpdated);
		assertFalse(itemWriter.isClosed);
	}

	@Test
	public void testIgnoreStream() throws Exception {
		ListItemStreamWriter itemWriter = new ListItemStreamWriter(writtenItems);
		writer.setDelegate(itemWriter);
		writer.setIgnoreItemStream(true);

		List<FutureTask<String>> processedItems = new ArrayList<FutureTask<String>>();

		ExecutionContext executionContext = new ExecutionContext();
		writer.open(executionContext);
		writer.write(processedItems);
		writer.update(executionContext);
		writer.close();

		assertFalse(itemWriter.isOpened);
		assertFalse(itemWriter.isUpdated);
		assertFalse(itemWriter.isClosed);
	}

	private class ListItemWriter implements ItemWriter<String> {

		protected List<String> items;
		public boolean isOpened = false;
		public boolean isUpdated = false;
		public boolean isClosed = false;

		public ListItemWriter(List<String> items) {
			this.items = items;
		}

		@Override
		public void write(List<? extends String> items) throws Exception {
			this.items.addAll(items);
		}
	}

	private class ListItemStreamWriter implements ItemStreamWriter<String> {
		public boolean isOpened = false;
		public boolean isUpdated = false;
		public boolean isClosed = false;
		protected List<String> items;

		public ListItemStreamWriter(List<String> items) {
			this.items = items;
		}

		@Override
		public void write(List<? extends String> items) throws Exception {
			this.items.addAll(items);
		}

		@Override
		public void open(ExecutionContext executionContext) throws ItemStreamException {
			isOpened = true;
		}

		@Override
		public void update(ExecutionContext executionContext) throws ItemStreamException {
			isUpdated = true;
		}

		@Override
		public void close() throws ItemStreamException {
			isClosed = true;
		}
	}
}
