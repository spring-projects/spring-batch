/*
 * Copyright 2014-2023 the original author or authors.
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
package org.springframework.batch.integration.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mminella
 * @author Mahmoud Ben Hassine
 */
class AsyncItemWriterTests {

	private AsyncItemWriter<String> writer;

	private List<String> writtenItems;

	private TaskExecutor taskExecutor;

	@BeforeEach
	void setup() {
		taskExecutor = new SimpleAsyncTaskExecutor();
		writtenItems = new ArrayList<>();
		writer = new AsyncItemWriter<>(new ListItemWriter(writtenItems));
	}

	@Test
	void testRoseyScenario() throws Exception {
		Chunk<FutureTask<String>> processedItems = new Chunk<>();

		processedItems.add(new FutureTask<>(() -> "foo"));

		processedItems.add(new FutureTask<>(() -> "bar"));

		for (FutureTask<String> processedItem : processedItems) {
			taskExecutor.execute(processedItem);
		}

		writer.write(processedItems);

		assertEquals(2, writtenItems.size());
		assertTrue(writtenItems.contains("foo"));
		assertTrue(writtenItems.contains("bar"));
	}

	@Test
	void testFilteredItem() throws Exception {
		Chunk<FutureTask<String>> processedItems = new Chunk<>();

		processedItems.add(new FutureTask<>(() -> "foo"));

		processedItems.add(new FutureTask<>(() -> null));

		for (FutureTask<String> processedItem : processedItems) {
			taskExecutor.execute(processedItem);
		}

		writer.write(processedItems);

		assertEquals(1, writtenItems.size());
		assertTrue(writtenItems.contains("foo"));
	}

	@Test
	void testException() {
		Chunk<FutureTask<String>> processedItems = new Chunk<>();

		processedItems.add(new FutureTask<>(() -> "foo"));

		processedItems.add(new FutureTask<>(() -> {
			throw new RuntimeException("This was expected");
		}));

		for (FutureTask<String> processedItem : processedItems) {
			taskExecutor.execute(processedItem);
		}

		Exception exception = assertThrows(RuntimeException.class, () -> writer.write(processedItems));
		assertEquals("This was expected", exception.getMessage());
	}

	@Test
	void testExecutionException() {
		Chunk<Future<String>> processedItems = new Chunk<>();

		processedItems.add(new Future<>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return false;
			}

			@Override
			public String get() throws InterruptedException, ExecutionException {
				throw new InterruptedException("expected");
			}

			@Override
			public String get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return null;
			}
		});

		Exception exception = assertThrows(Exception.class, () -> writer.write(processedItems));
		assertFalse(exception instanceof ExecutionException);

		assertEquals(0, writtenItems.size());
	}

	@Test
	void testStreamDelegate() throws Exception {
		ListItemStreamWriter itemWriter = new ListItemStreamWriter(writtenItems);
		writer.setDelegate(itemWriter);

		Chunk<FutureTask<String>> processedItems = new Chunk<>();

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
	void testNonStreamDelegate() throws Exception {
		ListItemWriter itemWriter = new ListItemWriter(writtenItems);
		writer.setDelegate(itemWriter);

		Chunk<FutureTask<String>> processedItems = new Chunk<>();

		ExecutionContext executionContext = new ExecutionContext();
		writer.open(executionContext);
		writer.write(processedItems);
		writer.update(executionContext);
		writer.close();

		assertFalse(itemWriter.isOpened);
		assertFalse(itemWriter.isUpdated);
		assertFalse(itemWriter.isClosed);
	}

	private static class ListItemWriter implements ItemWriter<String> {

		protected List<String> items;

		public boolean isOpened = false;

		public boolean isUpdated = false;

		public boolean isClosed = false;

		public ListItemWriter(List<String> items) {
			this.items = items;
		}

		@Override
		public void write(Chunk<? extends String> chunk) throws Exception {
			this.items.addAll(chunk.getItems());
		}

	}

	private static class ListItemStreamWriter implements ItemStreamWriter<String> {

		public boolean isOpened = false;

		public boolean isUpdated = false;

		public boolean isClosed = false;

		protected List<String> items;

		public ListItemStreamWriter(List<String> items) {
			this.items = items;
		}

		@Override
		public void write(Chunk<? extends String> chunk) throws Exception {
			this.items.addAll(chunk.getItems());
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
