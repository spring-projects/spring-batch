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
package org.springframework.batch.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class DirectPollerTests {

	private final Set<String> repository = new HashSet<>();

	@Test
	void testSimpleSingleThreaded() throws Exception {

		Callable<String> callback = () -> {
			Set<String> executions = new HashSet<>(repository);
			if (executions.isEmpty()) {
				return null;
			}
			return executions.iterator().next();
		};

		sleepAndCreateStringInBackground(500L);

		Future<String> task = new DirectPoller<String>(100L).poll(callback);

		String value = task.get(1000L, TimeUnit.MILLISECONDS);
		assertEquals("foo", value);

	}

	@Test
	void testTimeUnit() throws Exception {

		Callable<String> callback = () -> {
			Set<String> executions = new HashSet<>(repository);
			if (executions.isEmpty()) {
				return null;
			}
			return executions.iterator().next();
		};

		sleepAndCreateStringInBackground(500L);

		Future<String> task = new DirectPoller<String>(100L).poll(callback);

		String value = task.get(1L, TimeUnit.SECONDS);
		assertEquals("foo", value);

	}

	@Test
	void testWithError() {

		Callable<String> callback = () -> {
			Set<String> executions = new HashSet<>(repository);
			if (executions.isEmpty()) {
				return null;
			}
			throw new RuntimeException("Expected");
		};

		Poller<String> poller = new DirectPoller<>(100L);

		sleepAndCreateStringInBackground(500L);

		Exception exception = assertThrows(ExecutionException.class,
				() -> poller.poll(callback).get(1000L, TimeUnit.MILLISECONDS));
		assertEquals("Expected", exception.getCause().getMessage());
	}

	private void sleepAndCreateStringInBackground(long duration) {
		new Thread(() -> {
			try {
				Thread.sleep(duration);
				repository.add("foo");
			}
			catch (Exception e) {
				throw new IllegalStateException("Unexpected");
			}
		}).start();
	}

}
