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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.junit.Test;


/**
 * @author Dave Syer
 *
 */
public class ThrottleLimitResultQueueTests {
	
	private ThrottleLimitResultQueue<String> queue = new ThrottleLimitResultQueue<String>(1);
	
	@Test
	public void testPutTake() throws Exception {
		queue.expect();
		assertTrue(queue.isExpecting());
		assertTrue(queue.isEmpty());
		queue.put("foo");
		assertFalse(queue.isEmpty());
		assertEquals("foo", queue.take());
		assertFalse(queue.isExpecting());
	}

	@Test
	public void testPutWithoutExpecting() throws Exception {
		assertFalse(queue.isExpecting());
		try {
			queue.put("foo");
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testTakeWithoutExpecting() throws Exception {
		assertFalse(queue.isExpecting());
		try {
			queue.take();
			fail("Expected NoSuchElementException");
		} catch (NoSuchElementException e) {
			// expected
		}
	}

	@Test
	public void testThrottleLimit() throws Exception {
		queue.expect();
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(100L);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
				queue.put("foo");
			}
		}).start();
		long t0 = System.currentTimeMillis();
		queue.expect();
		long t1 = System.currentTimeMillis();
		assertEquals("foo", queue.take());
		assertTrue(queue.isExpecting());
		assertTrue("Did not block on expect (throttle limit should have been hit): time taken="+(t1-t0), t1-t0>50);
	}

}
