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
package org.springframework.batch.retry.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RetryState;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.support.DefaultRetryState;
import org.springframework.batch.retry.support.RetryTemplate;

/**
 * @author Dave Syer
 * 
 */
public class StatefulRetryIntegrationTests {

	@Test
	public void testExternalRetryWithFailAndNoRetry() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();

		RetryState retryState = new DefaultRetryState("foo");

		RetryTemplate retryTemplate = new RetryTemplate();
		MapRetryContextCache cache = new MapRetryContextCache();
		retryTemplate.setRetryContextCache(cache);
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));

		assertFalse(cache.containsKey("foo"));

		try {
			retryTemplate.execute(callback, retryState);
			// The first failed attempt we expect to retry...
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals(null, e.getMessage());
		}

		assertTrue(cache.containsKey("foo"));

		try {
			retryTemplate.execute(callback, retryState);
			// We don't get a second attempt...
			fail("Expected ExhaustedRetryException");
		}
		catch (ExhaustedRetryException e) {
			// This is now the "exhausted" message:
			assertNotNull(e.getMessage());
		}

		assertFalse(cache.containsKey("foo"));

		// Callback is called once: the recovery path should be called in
		// handleRetryExhausted (so not in this test)...
		assertEquals(1, callback.attempts);
	}

	@Test
	public void testExternalRetryWithSuccessOnRetry() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();

		RetryState retryState = new DefaultRetryState("foo");

		RetryTemplate retryTemplate = new RetryTemplate();
		MapRetryContextCache cache = new MapRetryContextCache();
		retryTemplate.setRetryContextCache(cache);
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));

		assertFalse(cache.containsKey("foo"));

		Object result = "start_foo";
		try {
			result = retryTemplate.execute(callback, retryState);
			// The first failed attempt we expect to retry...
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertNull(e.getMessage());
		}

		assertTrue(cache.containsKey("foo"));

		result = retryTemplate.execute(callback, retryState);

		assertFalse(cache.containsKey("foo"));

		assertEquals(2, callback.attempts);
		assertEquals("bar", result);
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static final class MockRetryCallback implements RetryCallback<String> {
		int attempts = 0;

		public String doWithRetry(RetryContext context) throws Exception {
			attempts++;
			if (attempts < 2) {
				throw new RuntimeException();
			}
			return "bar";
		}
	}

}
