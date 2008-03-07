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

import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.TerminatedRetryException;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.support.RetryTemplate;

public class ExternalRetryPolicyTests extends TestCase {

	public void testExternalRetryStopsLoop() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		callback.setExceptionToThrow(new IllegalArgumentException());

		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new MockExternalRetryPolicy(3));

		Object result = "start_foo";
		try {
			result = retryTemplate.execute(callback);
			// If template is external and retry is still permitted, then
			// we expect the exception to be propagated.
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertNull(e.getMessage());
		}
		assertEquals(1, callback.attempts);
		assertEquals("start_foo", result);
	}

	public void testExternalRetryWithFailAndNoRetry() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		callback.setExceptionToThrow(new IllegalArgumentException());

		RetryTemplate retryTemplate = new RetryTemplate();

		// Allow one unsuccessful attempt (plus one for recovery):
		retryTemplate.setRetryPolicy(new MockExternalRetryPolicy(1));

		Object result = "start_foo";
		try {
			result = retryTemplate.execute(callback);
			// The first failed attempt we expect to retry...
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertNull(e.getMessage());
		}

		try {
			result = retryTemplate.execute(callback);
			// We always get a second attempt...
		}
		catch (IllegalArgumentException e) {
			// This is now the "exhausted" message:
			assertNotNull(e.getMessage());
			// But if template is external we should
			// swallow the exception when retry is impossible.
			fail("Did not expect IllegalArgumentException");
		}
		// Callback is called once: the recovery path should be called in
		// handleRetryExhausted (so not in this test)...
		assertEquals(1, callback.attempts);
		assertEquals(null, result);
	}

	public void testNonThrowableIsNotRecoverable() throws Exception {

		try {
			MockExternalRetryPolicy policy = new MockExternalRetryPolicy(1);
			policy.setRecoverableExceptionClasses(new Class[] { String.class });
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// Expected
			System.err.println(e.getMessage());
			assertTrue(Pattern.matches(".*not.*Throwable.*", e.getMessage()));
		}

	}

	public void testSuclassIsRecoverable() throws Exception {

		MockExternalRetryPolicy policy = new MockExternalRetryPolicy(1);
		policy.setRecoverableExceptionClasses(new Class[] { IllegalArgumentException.class });

		RetryContextSupport context = new RetryContextSupport(null);

		assertTrue(policy.shouldRethrow(context));

		context.registerThrowable(new IllegalStateException());
		assertTrue(policy.shouldRethrow(context));

		context.registerThrowable(new IllegalArgumentException());
		assertFalse(policy.shouldRethrow(context));

		context.registerThrowable(new IllegalArgumentException() {
			// subclass
		});
		assertFalse(policy.shouldRethrow(context));

	}

	public void testRecoverableException() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		callback.setExceptionToThrow(new IllegalArgumentException());

		RetryTemplate retryTemplate = new RetryTemplate();

		// Allow one unsuccessful attempt (should take recovery path):
		MockExternalRetryPolicy policy = new MockExternalRetryPolicy(1);
		policy
				.setRecoverableExceptionClasses(new Class[] { IllegalArgumentException.class,
						IllegalStateException.class });
		retryTemplate.setRetryPolicy(policy);

		Object result = "start_foo";
		try {
			result = retryTemplate.execute(callback);
		}
		catch (IllegalArgumentException e) {
			// This is the "exhausted" message:
			assertNotNull(e.getMessage());
			// But if template is external we should
			// swallow the exception when retry is impossible.
			fail("Did not expect IllegalArgumentException");
		}
		// Callback is called once: the recovery path should be called in
		// handleRetryExhausted (so not in this test)...
		assertEquals(1, callback.attempts);
		assertEquals(null, result);
	}

	private static class MockRetryCallback implements RetryCallback {

		private int attempts;

		public static String EXHAUSTED = "complete";

		private Exception exceptionToThrow = new Exception();

		public Object doWithRetry(RetryContext context) throws Exception {
			this.attempts++;
			if (((Boolean) context.getAttribute(EXHAUSTED)).booleanValue()) {
				// This is now a recovery step...
				return null;
			}
			// Otherwise just barf...
			throw this.exceptionToThrow;
		}

		public void setExceptionToThrow(Exception exceptionToThrow) {
			this.exceptionToThrow = exceptionToThrow;
		}
	}

	private static class MockExternalRetryPolicy extends AbstractStatefulRetryPolicy {

		private int retryLimit = 0;

		// This one is stateful for testing only - normally this state would
		// have to be managed by the context.
		private int attempts = 0;

		public MockExternalRetryPolicy(int retryLimit) {
			super();
			this.retryLimit = retryLimit;
		}

		public boolean canRetry(RetryContext context) {
			return attempts < retryLimit;
		}

		public void close(RetryContext context) {
			// do nothing
		}

		public RetryContext open(RetryCallback callback) {
			RetryContextSupport context = new RetryContextSupport(null);
			context.setAttribute(MockRetryCallback.EXHAUSTED, Boolean.valueOf(!canRetry(context)));
			return context;
		}

		public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
			((RetryContextSupport) context).registerThrowable(throwable);
			attempts++;
		}

	}

}
