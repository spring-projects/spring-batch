/*
 * Copyright 2014 the original author or authors.
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
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertEquals;
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
		writer.setDelegate(new ListItemWriter(writtenItems));
	}

	@Test
	public void testRoseyScenario() throws Exception {
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

	private class ListItemWriter implements ItemWriter<String> {

		protected List<String> items;

		public ListItemWriter(List<String> items) {
			this.items = items;
		}

		@Override
		public void write(List<? extends String> items) throws Exception {
			this.items.addAll(items);
		}
	}
}
