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
package org.springframework.batch.infrastructure.repeat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.repeat.support.ThrottleLimitResultQueue;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@SuppressWarnings("removal")
class ThrottleLimitResultQueueTests {

	private final ThrottleLimitResultQueue<String> queue = new ThrottleLimitResultQueue<>(1);

	@Test
	void testPutTake() throws Exception {
		queue.expect();
		assertTrue(queue.isExpecting());
		assertTrue(queue.isEmpty());
		queue.put("foo");
		assertFalse(queue.isEmpty());
		assertEquals("foo", queue.take());
		assertFalse(queue.isExpecting());
	}

	@Test
	void testPutWithoutExpecting() {
		assertFalse(queue.isExpecting());
		assertThrows(IllegalArgumentException.class, () -> queue.put("foo"));
	}

	@Test
	void testTakeWithoutExpecting() {
		assertFalse(queue.isExpecting());
		assertThrows(NoSuchElementException.class, queue::take);
	}

	@Test
	void testThrottleLimit() throws Exception {
		queue.expect();
		new Thread(() -> {
			try {
				Thread.sleep(100L);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			queue.put("foo");
		}).start();
		long t0 = System.currentTimeMillis();
		queue.expect();
		long t1 = System.currentTimeMillis();
		assertEquals("foo", queue.take());
		assertTrue(queue.isExpecting());
		assertTrue(t1 - t0 > 50,
				"Did not block on expect (throttle limit should have been hit): time taken=" + (t1 - t0));
	}

}
