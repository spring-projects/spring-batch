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

import org.junit.Test;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.callback.RecoveryRetryCallback;
import org.springframework.batch.retry.support.RetryTemplate;

/**
 * @author Dave Syer
 * 
 */
public class ExternalRetryIntergrationTests {

	@Test
	public void testExternalRetryWithFailAndNoRetry() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();

		RecoveryRetryCallback recoveryCallback = new RecoveryRetryCallback("foo", callback);

		RetryTemplate retryTemplate = new RetryTemplate();
		RecoveryCallbackRetryPolicy retryPolicy = new RecoveryCallbackRetryPolicy(new SimpleRetryPolicy(1));
		MapRetryContextCache cache = new MapRetryContextCache();
		retryPolicy.setRetryContextCache(cache);
		retryTemplate.setRetryPolicy(retryPolicy);

		assertFalse(cache.containsKey("foo"));

		Object result = "start_foo";
		try {
			result = retryTemplate.execute(recoveryCallback);
			// The first failed attempt we expect to retry...
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertNull(e.getMessage());
		}

		assertTrue(cache.containsKey("foo"));

		try {
			result = retryTemplate.execute(recoveryCallback);
			// We always get a second attempt...
		}
		catch (IllegalArgumentException e) {
			// This is now the "exhausted" message:
			assertNotNull(e.getMessage());
			// But if template is external we should
			// swallow the exception when retry is impossible.
			fail("Did not expect IllegalArgumentException");
		}

		assertFalse(cache.containsKey("foo"));

		// Callback is called once: the recovery path should be called in
		// handleRetryExhausted (so not in this test)...
		assertEquals(1, callback.attempts);
		assertEquals(null, result);
	}

	@Test
	public void testExternalRetryWithSuccessOnRetry() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();

		RecoveryRetryCallback recoveryCallback = new RecoveryRetryCallback("foo", callback);

		RetryTemplate retryTemplate = new RetryTemplate();
		RecoveryCallbackRetryPolicy retryPolicy = new RecoveryCallbackRetryPolicy(new SimpleRetryPolicy(2));
		MapRetryContextCache cache = new MapRetryContextCache();
		retryPolicy.setRetryContextCache(cache);
		retryTemplate.setRetryPolicy(retryPolicy);

		assertFalse(cache.containsKey("foo"));

		Object result = "start_foo";
		try {
			result = retryTemplate.execute(recoveryCallback);
			// The first failed attempt we expect to retry...
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertNull(e.getMessage());
		}

		assertTrue(cache.containsKey("foo"));

		result = retryTemplate.execute(recoveryCallback);

		assertFalse(cache.containsKey("foo"));

		assertEquals(2, callback.attempts);
		assertEquals("bar", result);
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private final class MockRetryCallback implements RetryCallback {
		int attempts = 0;

		public Object doWithRetry(RetryContext context) throws Exception {
			attempts++;
			if (attempts < 2) {
				throw new RuntimeException();
			}
			return "bar";
		}
	}

}
