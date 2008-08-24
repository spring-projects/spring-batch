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

package org.springframework.batch.retry.callback;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.StubItemKeyGeneratorRecoverer;
import org.springframework.batch.retry.TerminatedRetryException;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;

public class RecoveryRetryCallbackTests extends TestCase {

	List<Object> calls = new ArrayList<Object>();

	int count = 0;

	RetryTemplate template;

	StubItemKeyGeneratorRecoverer recoverer;

	RecoveryRetryCallback callback;

	protected void setUp() throws Exception {
		super.setUp();
		template = new RetryTemplate();
		recoverer = new StubItemKeyGeneratorRecoverer() {
			public Object recover(Object data, Throwable cause) {
				count++;
				calls.add(data);
				return data;
			}

			public Object getKey(Object item) {
				return "key" + (count++);
			}
		};
		callback = new RecoveryRetryCallback("foo", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Exception {
				count++;
				return null;
			}
		});
	}

	public void testDoWithRetrySuccessfulFirstTime() throws Exception {
		template.execute(callback);
		assertEquals(1, count);
	}

	public void testContextInitializedWithItemAndCanRetry() throws Exception {
		// We can use the policy to intercept the context and do something with
		// the item...
		callback = new RecoveryRetryCallback("bar", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Exception {
				count++;
				throw new IllegalStateException("Detected bar");
			}
		});
		assertEquals(0, calls.size());
		template.setRetryPolicy(new NeverRetryPolicy() {
			public boolean canRetry(RetryContext context) {
				// ...register the failed item
				calls.add("item(" + count + ")=" + callback.getKey());
				// Do not call the base class method - the attempt counts as
				// successful now
				if (count < 2) // only retry once
					return true;
				return false;
			}
		});
		try {
			template.execute(callback);
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		System.err.println(calls);
		assertEquals(2, count);
		// Two from initial attempt (one in shouldRethrow and one at start of
		// do loop), two from the final attempt (same)...
		assertEquals(4, calls.size());
		assertEquals("item(1)=bar", calls.get(1));
	}

	public void testContextInitializedWithItemAndRegisterThrowable() throws Exception {
		// We can use the policy to intercept the context and do something with
		// the item...
		callback = new RecoveryRetryCallback("bar", new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Exception {
				count++;
				throw new IllegalStateException("Detected bar");
			}
		});
		assertEquals(0, calls.size());
		template.setRetryPolicy(new NeverRetryPolicy() {
			public void registerThrowable(RetryContext context, Exception throwable) throws TerminatedRetryException {
				// ...register the failed item
				calls.add("item=" + callback.getKey());
				// Call the base class method so that the next attempt is a
				// failure.
				super.registerThrowable(context, throwable);
			}
		});
		try {
			template.execute(callback);
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		// One call from the callback itself and one from the retry policy
		assertEquals(1, count);
		assertEquals(1, calls.size());
		assertEquals("item=bar", calls.get(0));
	}

	public void testContextMarkedExhausted() throws Exception {
		RetryContext context = new RetryContextSupport(null);
		context.setExhaustedOnly();
		try {
			callback.doWithRetry(context);
		}
		catch (Throwable t) {
			assertTrue(t instanceof RetryException);
		}
	}

	public void testGetKey() throws Exception {
		callback = new RecoveryRetryCallback("foo", null, "key0");
		assertEquals("key0", callback.getKey());
	}

	public void testRecoverWithoutSession() throws Exception {
		recoverer.recover("foo", null);
		assertEquals(1, count);
		assertEquals(1, calls.size());
	}
}
