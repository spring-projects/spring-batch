/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.poller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class DirectPollerTests {

	private Set<String> repository = new HashSet<String>();

	@Test
	public void testSimpleSingleThreaded() throws Exception {

		Callable<String> callback = new Callable<String>() {

			public String call() throws Exception {
				Set<String> executions = new HashSet<String>(repository);
				if (executions.isEmpty()) {
					return null;
				}
				return executions.iterator().next();
			}

		};

		sleepAndCreateStringInBackground(500L);

		Future<String> task = new DirectPoller<String>(100L).poll(callback);

		String value = task.get(1000L, TimeUnit.MILLISECONDS);
		assertEquals("foo", value);

	}

	@Test
	public void testTimeUnit() throws Exception {

		Callable<String> callback = new Callable<String>() {

			public String call() throws Exception {
				Set<String> executions = new HashSet<String>(repository);
				if (executions.isEmpty()) {
					return null;
				}
				return executions.iterator().next();
			}

		};

		sleepAndCreateStringInBackground(500L);

		Future<String> task = new DirectPoller<String>(100L).poll(callback);

		String value = task.get(1L, TimeUnit.SECONDS);
		assertEquals("foo", value);

	}

	@Test
	public void testWithError() throws Exception {

		Callable<String> callback = new Callable<String>() {

			public String call() throws Exception {
				Set<String> executions = new HashSet<String>(repository);
				if (executions.isEmpty()) {
					return null;
				}
				throw new RuntimeException("Expected");
			}

		};

		Poller<String> poller = new DirectPoller<String>(100L);

		sleepAndCreateStringInBackground(500L);

		try {
			String value = poller.poll(callback).get(1000L, TimeUnit.MILLISECONDS);
			assertEquals(null, value);
			fail("Expected ExecutionException");
		}
		catch (ExecutionException e) {
			assertEquals("Expected", e.getCause().getMessage());
		}

	}

	private void sleepAndCreateStringInBackground(final long duration) {
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(duration);
					repository.add("foo");
				}
				catch (Exception e) {
					throw new IllegalStateException("Unexpected");
				}
			}
		}).start();
	}

}
