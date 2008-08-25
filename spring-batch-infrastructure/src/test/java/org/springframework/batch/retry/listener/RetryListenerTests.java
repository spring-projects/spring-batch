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

package org.springframework.batch.retry.listener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.TerminatedRetryException;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;

public class RetryListenerTests extends TestCase {

	RetryTemplate template = new RetryTemplate();

	int count = 0;

	List<String> list = new ArrayList<String>();

	public void testOpenInterceptors() throws Exception {
		template.setListeners(new RetryListener[] { new RetryListenerSupport() {
			public <T> boolean open(RetryContext context, RetryCallback<T> callback) {
				count++;
				list.add("1:" + count);
				return true;
			}
		}, new RetryListenerSupport() {
			public <T> boolean open(RetryContext context, RetryCallback<T> callback) {
				count++;
				list.add("2:" + count);
				return true;
			}
		} });
		template.execute(new RetryCallback<String>() {
			public String doWithRetry(RetryContext context) throws Exception {
				return null;
			}
		});
		assertEquals(2, count);
		assertEquals(2, list.size());
		assertEquals("1:1", list.get(0));
	}

	public void testOpenCanVetoRetry() throws Exception {
		template.registerListener(new RetryListenerSupport() {
			public <T> boolean open(RetryContext context, RetryCallback<T> callback) {
				list.add("1");
				return false;
			}
		});
		try {
			template.execute(new RetryCallback<String>() {
				public String doWithRetry(RetryContext context) throws Exception {
					count++;
					return null;
				}
			});
			fail("Expected TerminatedRetryException");
		}
		catch (TerminatedRetryException e) {
			// expected
		}
		assertEquals(0, count);
		assertEquals(1, list.size());
		assertEquals("1", list.get(0));
	}

	public void testCloseInterceptors() throws Exception {
		template.setListeners(new RetryListener[] { new RetryListenerSupport() {
			public <T> void close(RetryContext context, RetryCallback<T> callback, Throwable t) {
				count++;
				list.add("1:" + count);
			}
		}, new RetryListenerSupport() {
			public <T> void close(RetryContext context, RetryCallback<T> callback, Throwable t) {
				count++;
				list.add("2:" + count);
			}
		} });
		template.execute(new RetryCallback<String>() {
			public String doWithRetry(RetryContext context) throws Exception {
				return null;
			}
		});
		assertEquals(2, count);
		assertEquals(2, list.size());
		// interceptors are called in reverse order on close...
		assertEquals("2:1", list.get(0));
	}

	public void testOnError() throws Exception {
		template.setRetryPolicy(new NeverRetryPolicy());
		template.setListeners(new RetryListener[] { new RetryListenerSupport() {
			public <T> void onError(RetryContext context, RetryCallback<T> callback, Throwable throwable) {
				list.add("1");
			}
		}, new RetryListenerSupport() {
			public <T> void onError(RetryContext context, RetryCallback<T> callback, Throwable throwable) {
				list.add("2");
			}
		} });
		try {
			template.execute(new RetryCallback<String>() {
				public String doWithRetry(RetryContext context) throws Exception {
					count++;
					throw new IllegalStateException("foo");
				}
			});
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			assertEquals("foo", e.getMessage());
		}
		// never retry so callback is executed once
		assertEquals(1, count);
		assertEquals(2, list.size());
		// interceptors are called in reverse order on error...
		assertEquals("2", list.get(0));

	}

	public void testCloseInterceptorsAfterRetry() throws Exception {
		template.registerListener(new RetryListenerSupport() {
			public <T> void close(RetryContext context, RetryCallback<T> callback, Throwable t) {
				list.add("" + count);
				// The last attempt should have been successful:
				assertNull(t);
			}
		});
		template.execute(new RetryCallback<String>() {
			public String doWithRetry(RetryContext context) throws Exception {
				if (count++ < 1)
					throw new RuntimeException("Retry!");
				return null;
			}
		});
		assertEquals(2, count);
		// The close interceptor was only called once:
		assertEquals(1, list.size());
		// We succeeded on the second try:
		assertEquals("2", list.get(0));
	}
}
