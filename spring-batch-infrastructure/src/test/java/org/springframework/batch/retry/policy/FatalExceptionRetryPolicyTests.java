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

import java.util.HashSet;

import junit.framework.TestCase;

import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryState;
import org.springframework.batch.retry.support.RetryTemplate;

public class FatalExceptionRetryPolicyTests extends TestCase {

	public void testFatalExceptionWithoutState() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		callback.setExceptionToThrow(new IllegalArgumentException());

		RetryTemplate retryTemplate = new RetryTemplate();
		// Allow multiple attempts in general...
		SimpleRetryPolicy policy = new SimpleRetryPolicy(3);
		retryTemplate.setRetryPolicy(policy);

		// ...but make sure certain exceptions are fatal
		policy.setFatalExceptionClasses(new HashSet<Class<? extends Throwable>>() {
			{
				add(IllegalArgumentException.class);
				add(IllegalStateException.class);
			}
		});
		RecoveryCallback recoveryCallback = new RecoveryCallback() {
			public Object recover(RetryContext context) throws Exception {
				return "bar";
			}
		};

		Object result = null;
		try {
			result = retryTemplate.execute(callback, recoveryCallback);
		}
		catch (IllegalArgumentException e) {
			// We should swallow the exception when recovery is possible
			fail("Did not expect IllegalArgumentException");
		}
		// Callback is called once: the recovery path should also be called
		assertEquals(1, callback.attempts);
		assertEquals("bar", result);
	}

	public void testFatalExceptionWithState() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		callback.setExceptionToThrow(new IllegalArgumentException());

		RetryTemplate retryTemplate = new RetryTemplate();
		SimpleRetryPolicy policy = new SimpleRetryPolicy(3);
		retryTemplate.setRetryPolicy(policy);

		policy.setFatalExceptionClasses(new HashSet<Class<? extends Throwable>>() {
			{
				add(IllegalArgumentException.class);
				add(IllegalStateException.class);
			}
		});
		RecoveryCallback recoveryCallback = new RecoveryCallback() {
			public Object recover(RetryContext context) throws Exception {
				return "bar";
			}
		};

		Object result = null;
		try {
			retryTemplate.execute(callback, recoveryCallback, new RetryState("foo"));
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// If stateful we have to always rethrow. Clients who want special
			// cases have to implement them in the callback
		}
		result = retryTemplate.execute(callback, recoveryCallback, new RetryState("foo"));
		// Callback is called once: the recovery path should also be called
		assertEquals(1, callback.attempts);
		assertEquals("bar", result);
	}

	private static class MockRetryCallback implements RetryCallback {

		private int attempts;

		private Exception exceptionToThrow = new Exception();

		public Object doWithRetry(RetryContext context) throws Exception {
			this.attempts++;
			// Otherwise just barf...
			throw this.exceptionToThrow;
		}

		public void setExceptionToThrow(Exception exceptionToThrow) {
			this.exceptionToThrow = exceptionToThrow;
		}
	}

}
