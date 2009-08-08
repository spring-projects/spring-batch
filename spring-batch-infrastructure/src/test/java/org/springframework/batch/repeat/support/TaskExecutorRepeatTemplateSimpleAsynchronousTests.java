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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.AbstractTradeBatchTests.TradeItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class TaskExecutorRepeatTemplateSimpleAsynchronousTests {

	static Log logger = LogFactory
			.getLog(TaskExecutorRepeatTemplateSimpleAsynchronousTests.class);

	private static int TOTAL = 100;

	@Test
	public void testThrottleLimit() throws Exception {

		int throttleLimit = 20;

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(300);
		template.setTaskExecutor(taskExecutor);
		template.setThrottleLimit(throttleLimit);

		final List<String> items = Collections
				.synchronizedList(new ArrayList<String>());

		final RepeatCallback callback = new RepeatCallback() {

			private volatile int count = 0;

			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {
				String item = count < TOTAL ? "" + count : null;
				count++;
				items.add("" + item);
				if (item != null) {
					beBusy();
				}
				return RepeatStatus.continueIf(item != null);
			}
		};

		template.iterate(callback);
		int frequency = Collections.frequency(items, "null");
//		System.err.println(items);
//		System.err.println("Frequency: " + frequency);
		assertEquals(TOTAL, items.size() - frequency);
		assertTrue(frequency > 1);
		assertTrue(frequency <= throttleLimit + 1);
	}

	@Test
	public void testThrottleLimitWithRetry() throws Exception {

		int throttleLimit = 30;

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(300);
		template.setTaskExecutor(taskExecutor);
		template.setThrottleLimit(throttleLimit);

		final List<String> items = Collections
				.synchronizedList(new ArrayList<String>());

		final RepeatCallback callback = new RepeatCallback() {

			private volatile AtomicInteger count = new AtomicInteger(0);
			private volatile int early = 2;

			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {

				int position = count.incrementAndGet();
				String item = position <= TOTAL ? "" + count : null;
				items.add("" + item);
				if (item != null) {
					beBusy();
				}
				/*
				 * In a multi-threaded task, one of the callbacks can call
				 * FINISHED early, while other threads are still working, and
				 * would do more work if the callback was called again. (This
				 * happens for instance if there is a failure and you want to
				 * retry the work.)
				 */
				RepeatStatus result = RepeatStatus.continueIf(position != early
						&& item != null);
				logger.debug("Returning " + result + " for count=" + position);
				return result;
			}
		};

		template.iterate(callback);
		int frequency = Collections.frequency(items, "null");
		assertEquals(TOTAL, items.size() - frequency);
		// System.err.println("Frequency: " + frequency);
		assertTrue(frequency > 1);
		assertTrue(frequency <= throttleLimit + 1);
	}

	@Test
	public void testThrottleLimitWithRetryAndEarlyCompletion() throws Exception {

		int throttleLimit = 30;

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(300);
		template.setCompletionPolicy(new SimpleCompletionPolicy(10));
		template.setTaskExecutor(taskExecutor);
		template.setThrottleLimit(throttleLimit);

		final List<String> items = Collections
				.synchronizedList(new ArrayList<String>());

		final RepeatCallback callback = new RepeatCallback() {

			private volatile AtomicInteger count = new AtomicInteger(0);
			private volatile int early = 2;

			public RepeatStatus doInIteration(RepeatContext context)
					throws Exception {

				int position = count.incrementAndGet();
				String item = position <= TOTAL ? "" + count : null;
				items.add("" + item);
				if (item != null) {
					beBusy();
				}
				RepeatStatus result = RepeatStatus.continueIf(position != early
						&& item != null);
				logger.debug("Returning " + result + " for count=" + position);
				return result;
			}
		};

		template.iterate(callback);
		int frequency = Collections.frequency(items, "null");
		assertEquals(10, items.size() - frequency);
		// System.err.println("Frequency: " + frequency);
		assertEquals(0, frequency);
	}

	private void beBusy() throws Exception {
		// Do some more I/O
		for (int i = 0; i < 10; i++) {
			TradeItemReader provider = new TradeItemReader(
					new ClassPathResource("trades.csv", getClass()));
			provider.open(new ExecutionContext());
			while (provider.read() != null)
				continue;
			provider.close();
		}

	}

}
