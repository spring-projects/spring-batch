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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RetryState;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.dao.DataAccessException;

public class StatefulRecoveryRetryTests {

	private RetryTemplate retryTemplate = new RetryTemplate();

	private int count = 0;

	private List<String> list = new ArrayList<String>();

	@Test
	public void testOpenSunnyDay() throws Exception {
		RetryContext context = retryTemplate.open(new NeverRetryPolicy(), new DefaultRetryState("foo"));
		assertNotNull(context);
		// we haven't called the processor yet...
		assertEquals(0, count);
	}

	@Test
	public void testRegisterThrowable() {
		NeverRetryPolicy retryPolicy = new NeverRetryPolicy();
		RetryState state = new DefaultRetryState("foo");
		RetryContext context = retryTemplate.open(retryPolicy, state);
		assertNotNull(context);
		retryTemplate.registerThrowable(retryPolicy, state, context, new Exception());
		assertFalse(retryPolicy.canRetry(context));
	}

	@Test
	public void testClose() throws Exception {
		NeverRetryPolicy retryPolicy = new NeverRetryPolicy();
		RetryState state = new DefaultRetryState("foo");
		RetryContext context = retryTemplate.open(retryPolicy, state);
		assertNotNull(context);
		retryTemplate.registerThrowable(retryPolicy, state, context, new Exception());
		assertFalse(retryPolicy.canRetry(context));
		retryTemplate.close(retryPolicy, context, state, true);
		// still can't retry, even if policy is closed
		// (not that this would happen in practice)...
		assertFalse(retryPolicy.canRetry(context));
	}

	@Test
	public void testRecoverWithParent() throws Exception {
		RepeatContext parent = new RepeatContextSupport(null);
		RepeatSynchronizationManager.register(new RepeatContextSupport(parent));
		testRecover();
		assertFalse(parent.isCompleteOnly());
		RepeatSynchronizationManager.clear();
	}

	@Test
	public void testRecover() throws Exception {
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));
		final String input = "foo";
		RetryState state = new DefaultRetryState(input);
		RetryCallback<String> callback = new RetryCallback<String>() {
			public String doWithRetry(RetryContext context) throws Exception {
				throw new RuntimeException("Barf!");
			}
		};
		RecoveryCallback<String> recoveryCallback = new RecoveryCallback<String>() {
			public String recover(RetryContext context) {
				count++;
				list.add(input);
				return input;
			}
		};
		Object result = null;
		try {
			result = retryTemplate.execute(callback, recoveryCallback, state);
			fail("Expected exception on first try");
		}
		catch (Exception e) {
			// expected...
		}
		// On the second retry, the recovery path is taken...
		result = retryTemplate.execute(callback, recoveryCallback, state);
		assertEquals(input, result); // default result is the item
		assertEquals(1, count);
		assertEquals(input, list.get(0));
	}

	@Test
	public void testSwitchToStatelessForNoRollback() throws Exception {
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));
		// Roll back for these:
		BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(Collections
				.<Class<? extends Throwable>> singleton(DataAccessException.class));
		// ...but not these:
		assertFalse(classifier.classify(new RuntimeException()));
		final String input = "foo";
		RetryState state = new DefaultRetryState(input, classifier);
		RetryCallback<String> callback = new RetryCallback<String>() {
			public String doWithRetry(RetryContext context) throws Exception {
				throw new RuntimeException("Barf!");
			}
		};
		RecoveryCallback<String> recoveryCallback = new RecoveryCallback<String>() {
			public String recover(RetryContext context) {
				count++;
				list.add(input);
				return input;
			}
		};
		Object result = null;
		// On the second retry, the recovery path is taken...
		result = retryTemplate.execute(callback, recoveryCallback, state);
		assertEquals(input, result); // default result is the item
		assertEquals(1, count);
		assertEquals(input, list.get(0));
	}

	@Test
	public void testExhaustedClearsHistoryAfterLastAttempt() throws Exception {
		RetryPolicy retryPolicy = new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true));
		retryTemplate.setRetryPolicy(retryPolicy);

		final String input = "foo";
		RetryState state = new DefaultRetryState(input);
		RetryCallback<String> callback = new RetryCallback<String>() {
			public String doWithRetry(RetryContext context) throws Exception {
				throw new RuntimeException("Barf!");
			}
		};

		try {
			retryTemplate.execute(callback, state);
			fail("Expected ExhaustedRetryException");
		}
		catch (RuntimeException e) {
			assertEquals("Barf!", e.getMessage());
		}

		try {
			retryTemplate.execute(callback, state);
			fail("Expected ExhaustedRetryException");
		}
		catch (ExhaustedRetryException e) {
			// expected
		}

		RetryContext context = retryTemplate.open(retryPolicy, state);
		// True after exhausted - the history is reset...
		assertTrue(retryPolicy.canRetry(context));
	}

	@Test
	public void testKeyGeneratorNotConsistentAfterFailure() throws Throwable {

		RetryPolicy retryPolicy = new SimpleRetryPolicy(3, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true));
		retryTemplate.setRetryPolicy(retryPolicy);
		final StringHolder item = new StringHolder("bar");
		RetryState state = new DefaultRetryState(item);

		RetryCallback<StringHolder> callback = new RetryCallback<StringHolder>() {
			public StringHolder doWithRetry(RetryContext context) throws Exception {
				// This simulates what happens if someone uses a primary key
				// for hashCode and equals and then relies on default key
				// generator
				((StringHolder) item).string = ((StringHolder) item).string + (count++);
				throw new RuntimeException("Barf!");
			}
		};

		try {
			retryTemplate.execute(callback, state);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException ex) {
			String message = ex.getMessage();
			assertEquals("Barf!", message);
		}
		// Only fails second attempt because the algorithm to detect
		// inconsistent has codes relies on the cache having been used for this
		// item already...
		try {
			retryTemplate.execute(callback, state);
			fail("Expected RetryException");
		}
		catch (RetryException ex) {
			String message = ex.getMessage();
			assertTrue("Message doesn't contain 'inconsistent': " + message, message.contains("inconsistent"));
		}

		RetryContext context = retryTemplate.open(retryPolicy, state);
		// True after exhausted - the history is reset...
		assertEquals(0, context.getRetryCount());

	}

	@Test
	public void testCacheCapacity() throws Exception {

		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));
		retryTemplate.setRetryContextCache(new MapRetryContextCache(1));

		RetryCallback<Object> callback = new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext context) throws Exception {
				count++;
				throw new RuntimeException("Barf!");
			}
		};

		try {
			retryTemplate.execute(callback, new DefaultRetryState("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Barf!", e.getMessage());
		}

		try {
			retryTemplate.execute(callback, new DefaultRetryState("bar"));
			fail("Expected RetryException");
		}
		catch (RetryException e) {
			String message = e.getMessage();
			assertTrue("Message does not contain 'capacity': " + message, message.indexOf("capacity") >= 0);
		}
	}

	@Test
	public void testCacheCapacityNotReachedIfRecovered() throws Exception {

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true));
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setRetryContextCache(new MapRetryContextCache(2));
		final StringHolder item = new StringHolder("foo");
		RetryState state = new DefaultRetryState(item);

		RetryCallback<Object> callback = new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext context) throws Exception {
				count++;
				throw new RuntimeException("Barf!");
			}
		};
		RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {
			public Object recover(RetryContext context) throws Exception {
				return null;
			}
		};

		try {
			retryTemplate.execute(callback, recoveryCallback, state);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Barf!", e.getMessage());
		}
		retryTemplate.execute(callback, recoveryCallback, state);

		RetryContext context = retryTemplate.open(retryPolicy, state);
		// True after exhausted - the history is reset...
		assertEquals(0, context.getRetryCount());

	}

	private static class StringHolder {

		private String string;

		public StringHolder(String string) {
			this.string = string;
		}

		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof StringHolder)) {
				return false;
			}
			return string.equals(((StringHolder) obj).string);
		}

		public int hashCode() {
			return string.hashCode();
		}

		public String toString() {
			return "String: " + string + " (hash = " + hashCode() + ")";
		}

	}

}
