/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.repeat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.callback.NestedRepeatCallback;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

class TaskExecutorRepeatTemplateAsynchronousTests extends AbstractTradeBatchTests {

	private final RepeatTemplate template = getRepeatTemplate();

	private int count = 0;

	private RepeatTemplate getRepeatTemplate() {
		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		template.setTaskExecutor(new SimpleAsyncTaskExecutor());
		// Set default completion above number of items in input file
		template.setCompletionPolicy(new SimpleCompletionPolicy(8));
		return template;
	}

	@Test
	void testEarlyCompletionWithException() {

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		template.setCompletionPolicy(new SimpleCompletionPolicy(20));
		taskExecutor.setConcurrencyLimit(2);
		template.setTaskExecutor(taskExecutor);
		Exception exception = assertThrows(IllegalStateException.class, () -> template.iterate(context -> {
			count++;
			throw new IllegalStateException("foo!");
		}));
		assertEquals("foo!", exception.getMessage());

		assertTrue(count >= 1, "Too few attempts: " + count);
		assertTrue(count <= 10, "Too many attempts: " + count);

	}

	@Test
	void testExceptionHandlerSwallowsException() {

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		template.setCompletionPolicy(new SimpleCompletionPolicy(4));
		taskExecutor.setConcurrencyLimit(2);
		template.setTaskExecutor(taskExecutor);

		template.setExceptionHandler((context, throwable) -> count++);
		template.iterate(context -> {
			throw new IllegalStateException("foo!");
		});

		assertTrue(count >= 1, "Too few attempts: " + count);
		assertTrue(count <= 10, "Too many attempts: " + count);

	}

	@Test
	void testNestedSession() {

		RepeatTemplate outer = getRepeatTemplate();
		RepeatTemplate inner = new RepeatTemplate();

		outer.iterate(new NestedRepeatCallback(inner, context -> {
			count++;
			assertNotNull(context);
			assertNotSame(context, context.getParent(), "Nested batch should have new session");
			assertSame(context, RepeatSynchronizationManager.getContext());
			return RepeatStatus.FINISHED;
		}) {
			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				assertNotNull(context);
				assertSame(context, RepeatSynchronizationManager.getContext());
				return super.doInIteration(context);
			}
		});

		assertTrue(count >= 1, "Too few attempts: " + count);
		assertTrue(count <= 10, "Too many attempts: " + count);

	}

	/**
	 * Run a batch with a single template that itself has an async task executor. The
	 * result is a batch that runs in multiple threads (up to the throttle limit of the
	 * template).
	 */
	@Test
	void testMultiThreadAsynchronousExecution() {

		final String threadName = Thread.currentThread().getName();
		final Set<String> threadNames = new HashSet<>();

		final RepeatCallback callback = context -> {
			assertNotSame(threadName, Thread.currentThread().getName());
			threadNames.add(Thread.currentThread().getName());
			Thread.sleep(100);
			Trade item = provider.read();
			if (item != null) {
				processor.write(Chunk.of(item));
			}
			return RepeatStatus.continueIf(item != null);
		};

		template.iterate(callback);
		// Shouldn't be necessary to wait:
		// Thread.sleep(500);
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		assertTrue(threadNames.size() > 1);
	}

	@Test
	@SuppressWarnings("removal")
	void testThrottleLimit() {

		int throttleLimit = 600;

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(300);
		template.setTaskExecutor(taskExecutor);

		String threadName = Thread.currentThread().getName();
		Set<String> threadNames = ConcurrentHashMap.newKeySet();
		List<String> items = Collections.synchronizedList(new ArrayList<>());

		RepeatCallback callback = context -> {
			assertNotSame(threadName, Thread.currentThread().getName());
			Trade item = provider.read();
			threadNames.add(Thread.currentThread().getName() + " : " + item);
			items.add(String.valueOf(item));
			if (item != null) {
				processor.write(Chunk.of(item));
				// Do some more I/O
				for (int i = 0; i < 10; i++) {
					TradeItemReader provider = new TradeItemReader(resource);
					provider.open(new ExecutionContext());
					while (provider.read() != null)
						continue;
					provider.close();
				}
			}
			return RepeatStatus.continueIf(item != null);
		};

		template.iterate(callback);
		// Shouldn't be necessary to wait:
		// Thread.sleep(500);
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		assertTrue(threadNames.size() > 1);
		int frequency = Collections.frequency(items, "null");
		assertTrue(frequency <= throttleLimit);
	}

	/**
	 * Wrap an otherwise synchronous batch in a callback to an asynchronous template.
	 */
	@Test
	void testSingleThreadAsynchronousExecution() {
		TaskExecutorRepeatTemplate jobTemplate = new TaskExecutorRepeatTemplate();
		final RepeatTemplate stepTemplate = new RepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(2);
		jobTemplate.setTaskExecutor(taskExecutor);

		final String threadName = Thread.currentThread().getName();
		final Set<String> threadNames = new HashSet<>();

		final RepeatCallback stepCallback = new ItemReaderRepeatCallback<>(provider, processor) {
			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				assertNotSame(threadName, Thread.currentThread().getName());
				threadNames.add(Thread.currentThread().getName());
				Thread.sleep(100);
				TradeItemReader provider = new TradeItemReader(resource);
				provider.open(new ExecutionContext());
				while (provider.read() != null)
					;
				return super.doInIteration(context);
			}
		};
		RepeatCallback jobCallback = context -> {
			stepTemplate.iterate(stepCallback);
			return RepeatStatus.FINISHED;
		};

		jobTemplate.iterate(jobCallback);
		// Shouldn't be necessary to wait:
		// Thread.sleep(500);
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		// Because of the throttling and queueing internally to a TaskExecutor,
		// more than one thread will be used - the number used is the
		// concurrency limit in the task executor, plus 1.
		assertTrue(threadNames.size() >= 1);
	}

}
