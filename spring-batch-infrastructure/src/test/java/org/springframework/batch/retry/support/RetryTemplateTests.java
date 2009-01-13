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

package org.springframework.batch.retry.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import static org.easymock.EasyMock.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.backoff.BackOffContext;
import org.springframework.batch.retry.backoff.BackOffInterruptedException;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.backoff.StatelessBackOffPolicy;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.support.BinaryExceptionClassifier;

/**
 * @author Rob Harrop
 * @author Dave Syer
 */
public class RetryTemplateTests {

	RetryContext context;

	int count = 0;

	@Test
	public void testSuccessfulRetry() throws Exception {
		for (int x = 1; x <= 10; x++) {
			MockRetryCallback callback = new MockRetryCallback();
			callback.setAttemptsBeforeSuccess(x);
			RetryTemplate retryTemplate = new RetryTemplate();
			retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
			retryTemplate.execute(callback);
			assertEquals(x, callback.attempts);
		}
	}

	@Test
	public void testSuccessfulRecovery() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		callback.setAttemptsBeforeSuccess(3);
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));
		final Object value = new Object();
		Object result = retryTemplate.execute(callback, new RecoveryCallback<Object>() {
			public Object recover(RetryContext context) throws Exception {
				return value;
			}
		});
		assertEquals(2, callback.attempts);
		assertEquals(value, result);
	}

	@Test
	public void testAlwaysTryAtLeastOnce() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new NeverRetryPolicy());
		retryTemplate.execute(callback);
		assertEquals(1, callback.attempts);
	}

	@Test
	public void testNoSuccessRetry() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		// Something that won't be thrown by JUnit...
		callback.setExceptionToThrow(new IllegalArgumentException());
		callback.setAttemptsBeforeSuccess(Integer.MAX_VALUE);
		RetryTemplate retryTemplate = new RetryTemplate();
		int retryAttempts = 2;
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(retryAttempts));
		try {
			retryTemplate.execute(callback);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertNotNull(e);
			assertEquals(retryAttempts, callback.attempts);
			return;
		}
		fail("Expected IllegalArgumentException");
	}

	@Test
	public void testDefaultConfigWithExceptionSubclass() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		int attempts = 3;
		callback.setAttemptsBeforeSuccess(attempts);
		callback.setExceptionToThrow(new IllegalArgumentException());

		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(attempts));
		retryTemplate.execute(callback);
		assertEquals(attempts, callback.attempts);
	}

	@Test
	public void testRollbackClassifierOverridesRetryPolicy() throws Exception {
		MockRetryCallback callback = new MockRetryCallback();
		int attempts = 3;
		callback.setAttemptsBeforeSuccess(attempts);
		callback.setExceptionToThrow(new IllegalArgumentException());

		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(attempts));
		BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(Collections
				.<Class<? extends Throwable>> singleton(IllegalArgumentException.class), false);
		retryTemplate.execute(callback, new DefaultRetryState("foo", classifier));
		assertEquals(attempts, callback.attempts);
	}

	@Test
	public void testSetExceptions() throws Exception {
		RetryTemplate template = new RetryTemplate();
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		template.setRetryPolicy(policy);
		policy.setRetryableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(RuntimeException.class));

		int attempts = 3;

		MockRetryCallback callback = new MockRetryCallback();
		callback.setAttemptsBeforeSuccess(attempts);

		try {
			template.execute(callback);
		}
		catch (Exception e) {
			assertNotNull(e);
			assertEquals(1, callback.attempts);
		}
		callback.setExceptionToThrow(new RuntimeException());

		template.execute(callback);
		assertEquals(attempts, callback.attempts);
	}

	@Test
	public void testBackOffInvoked() throws Exception {
		for (int x = 1; x <= 10; x++) {
			MockRetryCallback callback = new MockRetryCallback();
			MockBackOffStrategy backOff = new MockBackOffStrategy();
			callback.setAttemptsBeforeSuccess(x);
			RetryTemplate retryTemplate = new RetryTemplate();
			retryTemplate.setBackOffPolicy(backOff);
			retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
			retryTemplate.execute(callback);
			assertEquals(x, callback.attempts);
			assertEquals(1, backOff.startCalls);
			assertEquals(x - 1, backOff.backOffCalls);
		}
	}

	@Test
	public void testEarlyTermination() throws Exception {
		try {
			RetryTemplate retryTemplate = new RetryTemplate();
			retryTemplate.execute(new RetryCallback<Object>() {
				public Object doWithRetry(RetryContext status) throws Exception {
					status.setExhaustedOnly();
					throw new IllegalStateException("Retry this operation");
				}
			});
			fail("Expected ExhaustedRetryException");
		}
		catch (ExhaustedRetryException ex) {
			// Expected for internal retry policy (external would recover
			// gracefully)
			assertEquals("Retry this operation", ex.getCause().getMessage());
		}
	}

	@Test
	public void testNestedContexts() throws Exception {
		RetryTemplate outer = new RetryTemplate();
		final RetryTemplate inner = new RetryTemplate();
		outer.execute(new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext status) throws Exception {
				context = status;
				count++;
				Object result = inner.execute(new RetryCallback<Object>() {
					public Object doWithRetry(RetryContext status) throws Exception {
						count++;
						assertNotNull(context);
						assertNotSame(status, context);
						assertSame(context, status.getParent());
						assertSame("The context should be the child", status, RetrySynchronizationManager.getContext());
						return null;
					}
				});
				assertSame("The context should be restored", status, RetrySynchronizationManager.getContext());
				return result;
			}
		});
		assertEquals(2, count);
	}

	@Test
	public void testRethrowError() throws Exception {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new NeverRetryPolicy());
		try {
			retryTemplate.execute(new RetryCallback<Object>() {
				public Object doWithRetry(RetryContext context) throws Exception {
					throw new Error("Realllly bad!");
				}
			});
			fail("Expected Error");
		}
		catch (Error e) {
			assertEquals("Realllly bad!", e.getMessage());
		}
	}

	@Test
	public void testBackOffInterrupted() throws Exception {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setBackOffPolicy(new StatelessBackOffPolicy() {
			protected void doBackOff() throws BackOffInterruptedException {
				throw new BackOffInterruptedException("foo");
			}
		});
		try {
			retryTemplate.execute(new RetryCallback<Object>() {
				public Object doWithRetry(RetryContext context) throws Exception {
					throw new RuntimeException("Bad!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (BackOffInterruptedException e) {
			assertEquals("foo", e.getMessage());
		}
	}

	/**
	 * {@link BackOffPolicy} should apply also for exceptions that are
	 * re-thrown.
	 */
	@Test
	public void testBackOffForRethrownException() throws Exception {
		
		RetryTemplate tested = new RetryTemplate();
		tested.setRetryPolicy(new SimpleRetryPolicy(1));

		BackOffPolicy bop = createStrictMock(BackOffPolicy.class);
		BackOffContext backOffContext = new BackOffContext() {
		};
		tested.setBackOffPolicy(bop);

		expect(bop.start(isA(RetryContext.class))).andReturn(backOffContext);
		bop.backOff(backOffContext);
		expectLastCall().once();
		replay(bop);

		try {
			tested.execute(new RetryCallback<Object>() {

				public Object doWithRetry(RetryContext context) throws Exception {
					throw new Exception("maybe next time!");
				}

			}, null, new DefaultRetryState(tested) {

				@Override
				public boolean rollbackFor(Exception exception) {
					return true;
				}

			});
			fail();
		}
		catch (Exception expected) {
			assertEquals("maybe next time!", expected.getMessage());
		}
		
		verify(bop);
	}

	private static class MockRetryCallback implements RetryCallback<Object> {

		private int attempts;

		private int attemptsBeforeSuccess;

		private Exception exceptionToThrow = new Exception();

		public Object doWithRetry(RetryContext status) throws Exception {
			this.attempts++;
			if (attempts < attemptsBeforeSuccess) {
				throw this.exceptionToThrow;
			}
			return null;
		}

		public void setAttemptsBeforeSuccess(int attemptsBeforeSuccess) {
			this.attemptsBeforeSuccess = attemptsBeforeSuccess;
		}

		public void setExceptionToThrow(Exception exceptionToThrow) {
			this.exceptionToThrow = exceptionToThrow;
		}
	}

	private static class MockBackOffStrategy implements BackOffPolicy {

		public int backOffCalls;

		public int startCalls;

		public BackOffContext start(RetryContext status) {
			startCalls++;
			return null;
		}

		public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
			backOffCalls++;
		}
	}
}
