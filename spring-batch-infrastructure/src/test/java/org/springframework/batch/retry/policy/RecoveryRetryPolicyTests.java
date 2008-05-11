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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.callback.RecoveryRetryCallback;
import org.springframework.batch.retry.support.RetryTemplate;

public class RecoveryRetryPolicyTests extends TestCase {

	private RecoveryCallbackRetryPolicy policy = new RecoveryCallbackRetryPolicy();

	private int count = 0;

	private List list = new ArrayList();

	public void testOpenSunnyDay() throws Exception {

		final StringHolder item = new StringHolder("foo");
		RetryCallback writer = new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				list.add(item.string);
				return item;
			}
		};

		RetryContext context = policy.open(new RecoveryRetryCallback("foo", writer), null);
		assertNotNull(context);
		// we haven't called the processor yet...
		assertEquals(0, count);
	}

	public void testOpenWithWrongCallbackType() {
		try {
			policy.open(new RetryCallback() {
				public Object doWithRetry(RetryContext context) throws Throwable {
					return null;
				}
			}, null);
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().indexOf("must be ItemProvider") >= 0);
		}
	}

	public void testCanRetry() {
		policy.setDelegate(new AlwaysRetryPolicy());

		RetryContext context = policy.open(new RecoveryRetryCallback("foo", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				return null;
			}
		}), null);
		assertNotNull(context);

		// We can always retry if delegate says so...
		assertTrue(policy.canRetry(context));
	}

	public void testRegisterThrowable() {
		policy.setDelegate(new NeverRetryPolicy());
		RetryContext context = policy.open(new RecoveryRetryCallback("foo", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				return null;
			}
		}), null);
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
	}

	public void testClose() throws Exception {
		policy.setDelegate(new NeverRetryPolicy());
		RetryContext context = policy.open(new RecoveryRetryCallback("foo", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				return null;
			}
		}), null);
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
		policy.close(context);
		// still can't retry, even if policy is closed
		// (not that this would happen in practice)...
		assertFalse(policy.canRetry(context));
	}

	public void testOpenTwice() throws Exception {
		RecoveryRetryCallback callback = new RecoveryRetryCallback("foo", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				return null;
			}
		});
		policy.setDelegate(new SimpleRetryPolicy(2));

		// First call...
		RetryContext context = policy.open(callback, null);
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertTrue(policy.canRetry(context));
		policy.close(context);

		// Second call...
		context = policy.open(callback, null);
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
		policy.close(context);

	}

	public void testRecover() throws Exception {
		policy = new RecoveryCallbackRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		final String input = "foo";
		RecoveryRetryCallback callback = new RecoveryRetryCallback(input, new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				return null;
			}
		});
		callback.setRecoveryCallback(new RecoveryCallback() {
			public Object recover(RetryContext context) {
				count++;
				list.add(input);
				return input;
			}
		});
		RetryContext context = policy.open(callback, null);
		assertNotNull(context);
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
		assertEquals(0, count);
		context = policy.open(callback, null);
		// On the second retry, the recovery path is taken...
		Object result = policy.handleRetryExhausted(context);
		assertEquals("foo", result); // the recoverer returns the item
		assertEquals(1, count);
		assertFalse(policy.canRetry(context));
		assertEquals("foo", list.get(0));
	}

	public void testRecoverWithParent() throws Exception {
		RepeatContext parent = new RepeatContextSupport(null);
		RepeatSynchronizationManager.register(new RepeatContextSupport(parent));
		testRecover();
		assertFalse(parent.isCompleteOnly());
		RepeatSynchronizationManager.clear();
	}

	public void testRecoverWithTemplate() throws Exception {
		policy = new RecoveryCallbackRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		final String input = "foo";
		RecoveryRetryCallback callback = new RecoveryRetryCallback(input, new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				throw new RuntimeException("Barf!");
			}
		});
		callback.setRecoveryCallback(new RecoveryCallback() {
			public Object recover(RetryContext context) {
				count++;
				list.add(input);
				return input;
			}
		});
		RetryTemplate template = new RetryTemplate();
		template.setRetryPolicy(policy);
		Object result = null;
		try {
			result = template.execute(callback);
			fail("Expected exception on first try");
		}
		catch (Exception e) {
			// expected...
		}
		// On the second retry, the recovery path is taken...
		result = template.execute(callback);
		assertEquals(input, result); // default result is the item
		assertEquals(1, count);
		assertEquals(input, list.get(0));
	}

	public void testExhaustedClearsHistoryAfterLastAttempt() throws Exception {
		RecoveryRetryCallback callback = new RecoveryRetryCallback("foo", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				return null;
			}
		});
		policy.setDelegate(new SimpleRetryPolicy(1));

		RetryContext context = policy.open(callback, null);
		assertNotNull(context);

		assertEquals(0, count);
		policy.registerThrowable(context, new Exception());

		// False before close...
		assertFalse(policy.canRetry(context));
		policy.close(context);
		Object result = policy.handleRetryExhausted(context);
		assertNull(result); // default result is null

		context = policy.open(callback, null);
		// True after exhausted - the history is reset...
		assertTrue(policy.canRetry(context));
	}

	public void testRetryCount() throws Exception {
		policy = new RecoveryCallbackRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		RetryContext context = policy.open(new RecoveryRetryCallback("foo", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				return null;
			}
		}), null);
		assertNotNull(context);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		assertEquals("foo", context.getLastThrowable().getMessage());
	}

	public void testRetryCountPreservedBetweenRetries() throws Exception {
		RecoveryRetryCallback callback = new RecoveryRetryCallback("bar", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				return null;
			}
		});

		policy = new RecoveryCallbackRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		RetryContext context = policy.open(callback, null);
		assertNotNull(context);
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		context = policy.open(callback, null);
		assertEquals(1, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(2, context.getRetryCount());
	}

	public void testKeyGeneratorNotConsistentAfterFailure() throws Throwable {

		policy = new RecoveryCallbackRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(3));
		final StringHolder item = new StringHolder("bar");

		RetryCallback writer = new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				// This simulates what happens if someone uses a primary key
				// for hasCode and equals and then relies on default key
				// generator
				((StringHolder) item).string = ((StringHolder) item).string + (count++);
				throw new RuntimeException("Barf!");
			}
		};

		RecoveryRetryCallback callback = new RecoveryRetryCallback(item, writer);
		RetryContext context = policy.open(callback, null);
		assertNotNull(context);
		try {
			callback.doWithRetry(context);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Barf!", e.getMessage());
			try {
				policy.registerThrowable(context, e);
				fail("Expected RetryException");
			}
			catch (RetryException ex) {
				String message = ex.getMessage();
				assertTrue("Message doesn't contain 'inconsistent': " + message, message.indexOf("inconsistent") >= 0);
			}
			assertEquals(0, context.getRetryCount());
		}

	}

	public void testCacheCapacity() throws Exception {
		policy = new RecoveryCallbackRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		policy.setRetryContextCache(new MapRetryContextCache(1));
		final StringHolder item = new StringHolder("foo");

		RetryCallback writer = new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				list.add(item.string);
				return item;
			}
		};
		RetryContext context;
		context = policy.open(new RecoveryRetryCallback(item, writer), null);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		item.string = "bar";
		context = policy.open(new RecoveryRetryCallback(item, writer), null);
		try {
			policy.registerThrowable(context, new RuntimeException("foo"));
			fail("Expected RetryException");
		}
		catch (RetryException e) {
			String message = e.getMessage();
			assertTrue("Message does not contain 'capacity': " + message, message.indexOf("capacity") >= 0);
		}
	}

	public void testCacheCapacityNotReachedIfRecovered() throws Exception {
		policy = new RecoveryCallbackRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		policy.setRetryContextCache(new MapRetryContextCache(2));
		final StringHolder item = new StringHolder("foo");

		RetryCallback writer = new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				count++;
				list.add(item.string);
				return item;
			}
		};

		RetryContext context;
		context = policy.open(new RecoveryRetryCallback(item, writer), null);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		context = policy.open(new RecoveryRetryCallback("bar", writer), null);
		policy.registerThrowable(context, null);
		policy.handleRetryExhausted(context);
		context = policy.open(new RecoveryRetryCallback("spam", writer), null);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
	}

	private static class StringHolder {

		private String string;

		/**
		 * @param string
		 */
		public StringHolder(String string) {
			this.string = string;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return string.equals(((StringHolder) obj).string);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return string.hashCode();
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "String: " + string + " (hash = " + hashCode() + ")";
		}

	}

}
