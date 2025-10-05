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

package org.springframework.batch.infrastructure.repeat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatCallback;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.policy.SimpleCompletionPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Simple tests for concurrent behaviour in repeat template, in particular the barrier at
 * the end of the iteration. N.B. these tests may fail if insufficient threads are
 * available (e.g. on a single-core machine, or under load). They shouldn't deadlock
 * though.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class TaskExecutorRepeatTemplateBulkAsynchronousTests {

	static Log logger = LogFactory.getLog(TaskExecutorRepeatTemplateBulkAsynchronousTests.class);

	private int total = 1000;

	private volatile int early = Integer.MAX_VALUE;

	private volatile int error = Integer.MAX_VALUE;

	private TaskExecutorRepeatTemplate template;

	private RepeatCallback callback;

	private List<String> items;

	private final ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();

	@BeforeEach
	void setUp() {

		template = new TaskExecutorRepeatTemplate();
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
