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
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.provider.ListItemReader;
import org.springframework.batch.retry.ListItemReaderRecoverer;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.exception.RetryException;
import org.springframework.batch.retry.exception.TerminatedRetryException;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;

public class ItemReaderRetryCallbackTests extends TestCase {

	List calls = new ArrayList();

	int count = 0;

	RetryTemplate template;

	ListItemReader provider;

	ItemReaderRetryCallback callback;

	protected void setUp() throws Exception {
		super.setUp();
		template = new RetryTemplate();
		provider = new ListItemReaderRecoverer(Arrays.asList(new String[] { "foo", "bar" })) {
			public boolean recover(Object data, Throwable cause) {
				count++;
				calls.add(data);
				return true;
			}
			public Object getKey(Object item) {
				return "key" + (count++);
			}
		};
		callback = new ItemReaderRetryCallback(provider, new ItemProcessor() {
			public void process(Object data) {
				count++;
				if (data.equals("bar")) {
					throw new IllegalStateException("Bar detected");
				}
			}
		});
	}

	public void testDoWithRetrySuccessfulFirstTime() throws Exception {
		template.execute(callback);
		assertEquals(1, count);
	}

	public void testDataExhausted() throws Exception {
		provider.read();
		provider.read(); // line up a null data item...

		try {
			template.execute(callback);
		}
		catch (RetryException e) {
			fail("Unexpected RetryException");
		}

		// The item is null, and is not processed:
		assertEquals(0, count);
	}

	public void testContextInitializedWithItemAndCanRetry() throws Exception {
		// We can use the policy to intercept the context and do something with
		// the item...
		provider.read(); // line up an unsuccessful call...
		assertEquals(0, calls.size());
		template.setRetryPolicy(new NeverRetryPolicy() {
			public boolean canRetry(RetryContext context) {
				// ...register the failed item
				calls.add("item(" + count + ")=" + context.getAttribute(ItemReaderRetryCallback.ITEM));
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
		provider.read(); // line up an unsuccessful call...
		assertEquals(0, calls.size());
		template.setRetryPolicy(new NeverRetryPolicy() {
			public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
				// ...register the failed item
				calls.add("item=" + context.getAttribute(ItemReaderRetryCallback.ITEM));
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
		// One call from the callback itslef and one from the retry policy
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
		assertEquals("key0", callback.getProvider().getKey("foo"));
	}

	public void testRecoverWithoutSession() throws Exception {
		callback.getRecoverer().recover("foo", null);
		assertEquals(1, count);
		assertEquals(1, calls.size());
	}
}
