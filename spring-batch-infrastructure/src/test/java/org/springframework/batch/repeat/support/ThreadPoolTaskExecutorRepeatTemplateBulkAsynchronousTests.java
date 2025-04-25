/*
 * Copyright 2006-2023 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests for concurrent behaviour in repeat template, in particular the barrier at
 * the end of the iteration. N.B. these tests may fail if insufficient threads are
 * available (e.g. on a single-core machine, or under load). They shouldn't deadlock
 * though.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Linus Yan
 *
 */
class ThreadPoolTaskExecutorRepeatTemplateBulkAsynchronousTests {

	static Log logger = LogFactory.getLog(ThreadPoolTaskExecutorRepeatTemplateBulkAsynchronousTests.class);

	private int total = 1000;

	private int throttleLimit = 30;

	private volatile int early = Integer.MAX_VALUE;

	private volatile int error = Integer.MAX_VALUE;

	private ThreadPoolTaskExecutorRepeatTemplate template;

	private RepeatCallback callback;

	private List<String> items;

	private final ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();

	@BeforeEach
	void setUp() {

		template = new ThreadPoolTaskExecutorRepeatTemplate();
		threadPool.setMaxPoolSize(300);
		threadPool.setCorePoolSize(10);
		threadPool.setQueueCapacity(0);
		threadPool.afterPropertiesSet();
		template.setTaskExecutor(threadPool);

		items = Collections.synchronizedList(new ArrayList<>());

		callback = new RepeatCallback() {

			private final AtomicInteger count = new AtomicInteger(0);

			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				int position = count.incrementAndGet();
				String item = position <= total ? String.valueOf(position) : null;
				items.add(item);
				if (item != null) {
					beBusy();
				}
				/*
				 * In a multi-threaded task, one of the callbacks can call FINISHED early,
				 * while other threads are still working, and would do more work if the
				 * callback was called again. (This happens for instance if there is a
				 * failure and you want to retry the work.)
				 */
				RepeatStatus result = RepeatStatus.continueIf(position != early && item != null);
				if (position == error) {
					throw new RuntimeException("Planned");
				}
				if (!result.isContinuable()) {
					logger.debug("Returning " + result + " for count=" + position);
				}
				return result;
			}
		};

	}

	@AfterEach
	void tearDown() {
		threadPool.destroy();
	}

	@Test
	void testThrottleLimit() {

		template.iterate(callback);
		int frequency = Collections.frequency(items, null);
		assertEquals(total, items.size() - frequency);
		assertTrue(frequency > 1);
		assertTrue(frequency <= throttleLimit + 1);

	}

	@Test
	void testThrottleLimitEarlyFinish() {

		early = 2;

		template.iterate(callback);
		int frequency = Collections.frequency(items, null);
		assertEquals(total, items.size() - frequency);
		assertTrue(frequency > 1);
		assertTrue(frequency <= throttleLimit + 1);

	}

	@Test
	void testThrottleLimitEarlyFinishThreadStarvation() {

		early = 2;
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		// Set the concurrency limit below the throttle limit for possible
		// starvation condition
		taskExecutor.setMaxPoolSize(20);
		taskExecutor.setCorePoolSize(10);
		taskExecutor.setQueueCapacity(0);
		// This is the most sensible setting, otherwise the bookkeeping in
		// ResultHolderResultQueue gets out of whack when tasks are aborted.
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		taskExecutor.afterPropertiesSet();
		template.setTaskExecutor(taskExecutor);

		template.iterate(callback);
		int frequency = Collections.frequency(items, null);
		// Extra tasks will be submitted before the termination is detected
		assertEquals(total, items.size() - frequency);
		assertTrue(frequency <= throttleLimit + 1);

		taskExecutor.destroy();

	}

	@Test
	void testThrottleLimitEarlyFinishOneThread() {

		early = 4;
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

		// This is kind of slow with only one thread, so reduce size:
		throttleLimit = 10;
		total = 20;

		template.setTaskExecutor(taskExecutor);

		template.iterate(callback);
		int frequency = Collections.frequency(items, null);
		assertEquals(total, items.size() - frequency);
		assertTrue(frequency <= throttleLimit + 1);

	}

	@Test
	void testThrottleLimitWithEarlyCompletion() {

		early = 2;
		template.setCompletionPolicy(new SimpleCompletionPolicy(10));

		template.iterate(callback);
		int frequency = Collections.frequency(items, null);
		assertEquals(10, items.size() - frequency);
		assertEquals(0, frequency);

	}

	@Test
	void testThrottleLimitWithError() {

		error = 50;

		Exception exception = assertThrows(Exception.class, () -> template.iterate(callback));
		assertEquals("Planned", exception.getMessage());
		int frequency = Collections.frequency(items, null);
		assertEquals(0, frequency);

	}

	@Test
	void testErrorThrownByCallback() {

		callback = new RepeatCallback() {

			private final AtomicInteger count = new AtomicInteger(0);

			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				int position = count.incrementAndGet();

				if (position == 4) {
					throw new OutOfMemoryError("Planned");
				}
				else {
					return RepeatStatus.CONTINUABLE;
				}
			}
		};

		template.setCompletionPolicy(new SimpleCompletionPolicy(10));

		Error error = assertThrows(OutOfMemoryError.class, () -> template.iterate(callback));
		assertEquals("Planned", error.getMessage());
	}

	/**
	 * Slightly flakey convenience method. If this doesn't do something that lasts
	 * sufficiently long for another worker to be launched while it is busy, the early
	 * completion tests will fail. "Sufficiently long" is the problem so we try and block
	 * until we know someone else is busy?
	 * @throws Exception if interrupted while being busy
	 */
	private void beBusy() throws Exception {
		synchronized (this) {
			wait(100L);
			notifyAll();
		}
	}

}
