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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.FailedItemIdentifier;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.retry.ListItemReaderRecoverer;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.support.RetryTemplate;

public class ItemReaderRetryPolicyTests extends TestCase {

	private ItemReaderRetryPolicy policy = new ItemReaderRetryPolicy();

	private ItemReader reader;

	private int count = 0;

	private List list = new ArrayList();

	private ItemKeyGenerator keyGenerator = new ItemKeyGenerator() {
		public Object getKey(Object item) {
			return item;
		}
	};

	protected void setUp() throws Exception {
		super.setUp();
		// The list simulates a failed delivery, redelivery of the same message,
		// then a new message...
		reader = new ListItemReaderRecoverer(Arrays.asList(new String[] { "foo", "foo", "bar" })) {
			public boolean recover(Object data, Throwable cause) {
				count++;
				list.add(data);
				return true;
			}
		};
	}

	public void testOpenSunnyDay() throws Exception {
		RetryContext context = policy.open(new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
				list.add(data);
			}
		}));
		assertNotNull(context);
		// we haven't called the processor yet...
		assertEquals(0, count);
		// but the provider has been accessed:
		assertEquals("foo", reader.read());
		assertEquals("bar", reader.read());
	}

	public void testOpenWithWrongCallbackType() {
		try {
			policy.open(new RetryCallback() {
				public Object doWithRetry(RetryContext context) throws Throwable {
					return null;
				}
			});
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().indexOf("must be ItemProvider") >= 0);
		}
	}

	public void testCanRetry() {
		policy.setDelegate(new AlwaysRetryPolicy());

		RetryContext context = policy.open(new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
			}
		}));
		assertNotNull(context);

		// We can always retry if delegate says so...
		assertTrue(policy.canRetry(context));
	}

	public void testRegisterThrowable() {
		policy.setDelegate(new NeverRetryPolicy());
		RetryContext context = policy.open(new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
				list.add(data);
			}
		}));
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
	}

	public void testClose() throws Exception {
		policy.setDelegate(new NeverRetryPolicy());
		RetryContext context = policy.open(new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
				list.add(data);
			}
		}));
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
		policy.close(context);
		// still can't retry, even if policy is closed
		// (not that this would happen in practice)...
		assertFalse(policy.canRetry(context));
		// The provider has been accessed only once:
		assertEquals("foo", reader.read());
		assertEquals("bar", reader.read());
	}

	public void testOpenTwice() throws Exception {
		ItemReaderRetryCallback callback = new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
				list.add(data);
			}
		});
		policy.setDelegate(new SimpleRetryPolicy(2));

		// First call...
		RetryContext context = policy.open(callback);
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertTrue(policy.canRetry(context));
		policy.close(context);

		// Second call...
		context = policy.open(callback);
		assertNotNull(context);
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
		policy.close(context);

		// The provider has been accessed twice, so this
		// mimics a message receive by repeating the value of the first
		// message...
		assertEquals("bar", reader.read());
	}

	public void testRecover() throws Exception {
		policy = new ItemReaderRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		ItemReaderRetryCallback callback = new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
			}
		});
		RetryContext context = policy.open(callback);
		assertNotNull(context);
		assertTrue(policy.canRetry(context));
		policy.registerThrowable(context, new Exception());
		assertFalse(policy.canRetry(context));
		assertEquals(0, count);
		context = policy.open(callback);
		// On the second retry, the recovery path is taken...
		Object result = policy.handleRetryExhausted(context);
		assertNotNull(result); // default result is null
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

	public void testFailedItemIdentifier() throws Exception {
		policy = new ItemReaderRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		MockFailedItemProvider provider = new MockFailedItemProvider(Collections.EMPTY_LIST);
		ItemReaderRetryCallback callback = new ItemReaderRetryCallback(provider, null);
		policy.open(callback);
		assertEquals(1, provider.hasFailedCount);
	}

	public void testRecoverWithTemplate() throws Exception {
		policy = new ItemReaderRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		ItemReaderRetryCallback callback = new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				throw new RuntimeException("Barf!");
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
		assertNotNull(result); // default result is last item processed
		assertEquals(1, count);
		assertEquals("foo", list.get(0));
	}

	public void testExhaustedClearsHistoryAfterLastAttempt() throws Exception {
		ItemReaderRetryCallback callback = new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
				list.add(data);
			}
		});
		policy.setDelegate(new SimpleRetryPolicy(1));

		RetryContext context = policy.open(callback);
		assertNotNull(context);

		assertEquals(0, count);
		policy.registerThrowable(context, new Exception());

		// False before close...
		assertFalse(policy.canRetry(context));
		policy.close(context);
		Object result = policy.handleRetryExhausted(context);
		assertEquals("foo", result); // default result is last item

		context = policy.open(callback);
		// True after exhausted - the history is reset...
		assertTrue(policy.canRetry(context));
	}

	public void testRetryCount() throws Exception {
		policy = new ItemReaderRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		RetryContext context = policy.open(new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
				list.add(data);
			}
		}));
		assertNotNull(context);
		policy.registerThrowable(context, null);
		assertEquals(0, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		assertEquals("foo", context.getLastThrowable().getMessage());
	}

	public void testRetryCountPreservedBetweenRetries() throws Exception {
		ItemReaderRetryCallback callback = new ItemReaderRetryCallback(reader, new AbstractItemWriter() {
			public void write(Object data) {
				count++;
				list.add(data);
			}
		});

		policy = new ItemReaderRetryPolicy();
		policy.setDelegate(new SimpleRetryPolicy(1));
		RetryContext context = policy.open(callback);
		assertNotNull(context);
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(1, context.getRetryCount());
		context = policy.open(callback);
		assertEquals(1, context.getRetryCount());
		policy.registerThrowable(context, new RuntimeException("foo"));
		assertEquals(2, context.getRetryCount());
	}

	public void testSetCacheAndHasFailed() throws Exception {
		MapRetryContextCache cache = new MapRetryContextCache();
		policy.setRetryContextCache(cache);
		cache.put("foo", new RetryContextSupport(null));
		assertTrue(policy.hasFailed(reader, keyGenerator , "foo"));
	}

	private static class MockFailedItemProvider extends ListItemReader implements ItemKeyGenerator, FailedItemIdentifier {

		private int hasFailedCount = 0;

		public MockFailedItemProvider(List list) {
			super(list);
		}

		public boolean hasFailed(Object item) {
			hasFailedCount++;
			return false;
		}

		public Object getKey(Object item) {
			throw new UnsupportedOperationException("Should not call this method");
		}

	}

}
