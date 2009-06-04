/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.repeat.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class TaskExecutorRepeatTemplateAsynchronousTests extends
		AbstractTradeBatchTests {

	RepeatTemplate template = getRepeatTemplate();

	int count = 0;

	// @Override
	public RepeatTemplate getRepeatTemplate() {
		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		template.setTaskExecutor(new SimpleAsyncTaskExecutor());
		return template;
	}

	@Test
	public void testEarlyCompletionWithException() throws Exception {

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		template.setCompletionPolicy(new SimpleCompletionPolicy(20));
		taskExecutor.setConcurrencyLimit(2);
		template.setTaskExecutor(taskExecutor);
		try {
			template.iterate(new RepeatCallback() {
				public RepeatStatus doInIteration(RepeatContext context)
						throws Exception {
					count++;
					throw new IllegalStateException("foo!");
				}
			});
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertEquals("foo!", e.getMessage());
		}

		assertTrue("Too few attempts: "+count, count>=2);
		assertTrue("Too many attempts: "+count, count<=10);

	}

	@Test
	public void testExceptionHandlerSwallowsException() throws Exception {

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		template.setCompletionPolicy(new SimpleCompletionPolicy(4));
		taskExecutor.setConcurrencyLimit(2);
		template.setTaskExecutor(taskExecutor);

		template.setExceptionHandler(new ExceptionHandler() {
			public void handleException(RepeatContext context,
					Throwable throwable) throws Throwable {
				count++;
			}
		});
		template.iterate(new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {
				throw new IllegalStateException("foo!");
			}
		});

		assertEquals(4, count);

	}

	/**
	 * Run a batch with a single template that itself has an async task
	 * executor. The result is a batch that runs in multiple threads (up to the
	 * throttle limit of the template).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMultiThreadAsynchronousExecution() throws Exception {

		final String threadName = Thread.currentThread().getName();
		final Set<String> threadNames = new HashSet<String>();

		final RepeatCallback callback = new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {
				assertNotSame(threadName, Thread.currentThread().getName());
				threadNames.add(Thread.currentThread().getName());
				Thread.sleep(100);
				Trade item = provider.read();
				if (item != null) {
					processor.write(Collections.singletonList(item));
				}
				return RepeatStatus.continueIf(item != null);
			}
		};

		template.iterate(callback);
		// Shouldn't be necessary to wait:
		// Thread.sleep(500);
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		assertTrue(threadNames.size() > 1);
	}

	@Test
	public void testThrottleLimit() throws Exception {
		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(3);
		template.setTaskExecutor(taskExecutor);
		template.setThrottleLimit(12);

		final String threadName = Thread.currentThread().getName();
		final Set<String> threadNames = new HashSet<String>();

		final RepeatCallback callback = new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {
				assertNotSame(threadName, Thread.currentThread().getName());
				threadNames.add(Thread.currentThread().getName());
				Trade item = provider.read();
				if (item != null) {
					processor.write(Collections.singletonList(item));
				}
				return RepeatStatus.continueIf(item != null);
			}
		};

		template.iterate(callback);
		// Shouldn't be necessary to wait:
		// Thread.sleep(500);
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		assertTrue(threadNames.size() > 1);
	}

	/**
	 * Wrap an otherwise synchronous batch in a callback to an asynchronous
	 * template.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleThreadAsynchronousExecution() throws Exception {
		TaskExecutorRepeatTemplate jobTemplate = new TaskExecutorRepeatTemplate();
		final RepeatTemplate stepTemplate = new RepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(2);
		jobTemplate.setTaskExecutor(taskExecutor);

		final String threadName = Thread.currentThread().getName();
		final Set<String> threadNames = new HashSet<String>();

		final RepeatCallback stepCallback = new ItemReaderRepeatCallback<Trade>(
				provider, processor) {
			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {
				assertNotSame(threadName, Thread.currentThread().getName());
				threadNames.add(Thread.currentThread().getName());
				Thread.sleep(100);
				return super.doInIteration(context);
			}
		};
		RepeatCallback jobCallback = new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {
				stepTemplate.iterate(stepCallback);
				return RepeatStatus.FINISHED;
			}
		};

		jobTemplate.iterate(jobCallback);
		// Shouldn't be necessary to wait:
		// Thread.sleep(500);
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		// Because of the throttling and queueing internally to a TaskExecutor,
		// more than one thread will be used - the number used is the
		// concurrency limit in the task executor, plus 1.
		// System.err.println(threadNames);
		assertTrue(threadNames.size() >= 1);
	}

	// TODO: test transactional callback with async template.

}
