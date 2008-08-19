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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class AsynchronousRepeatTests extends AbstractTradeBatchTests {

	/**
	 * Run a batch with a single template that itself has an asynch task
	 * executor. The result is a batch that runs in multiple threads (up to the
	 * throttle limit of the template).
	 * 
	 * @throws Exception
	 */
	public void testMultiThreadAsynchronousExecution() throws Exception {
		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		template.setTaskExecutor(new SimpleAsyncTaskExecutor());

		final String threadName = Thread.currentThread().getName();
		final Set<String> threadNames = new HashSet<String>();

		final RepeatCallback callback = new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				assertNotSame(threadName, Thread.currentThread().getName());
				threadNames.add(Thread.currentThread().getName());
				Thread.sleep(100);
				Trade item = provider.read();
				if (item!=null) {
					processor.write(Collections.singletonList(item));
				}
				return new ExitStatus(item!=null);
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
	public void testSingleThreadAsynchronousExecution() throws Exception {
		TaskExecutorRepeatTemplate jobTemplate = new TaskExecutorRepeatTemplate();
		final RepeatTemplate stepTemplate = new RepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(2);
		jobTemplate.setTaskExecutor(taskExecutor);

		final String threadName = Thread.currentThread().getName();
		final Set<String> threadNames = new HashSet<String>();

		final RepeatCallback stepCallback = new ItemReaderRepeatCallback<Trade>(provider, processor) {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				assertNotSame(threadName, Thread.currentThread().getName());
				threadNames.add(Thread.currentThread().getName());
				Thread.sleep(100);
				return super.doInIteration(context);
			}
		};
		RepeatCallback jobCallback = new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				stepTemplate.iterate(stepCallback);
				return ExitStatus.FINISHED;
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

	// TODO: test transactional callback with asynch template.

}
