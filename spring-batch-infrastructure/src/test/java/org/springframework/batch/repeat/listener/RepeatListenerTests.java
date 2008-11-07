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

package org.springframework.batch.repeat.listener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class RepeatListenerTests extends TestCase {

	int count = 0;

	public void testBeforeInterceptors() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void before(RepeatContext context) {
				calls.add("1");
			}
		}, new RepeatListenerSupport() {
			public void before(RepeatContext context) {
				calls.add("2");
			}
		} });
		template.iterate(new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				return RepeatStatus.continueIf(count <= 1);
			}
		});
		// 2 calls: the second time there is no processing
		// (despite the fact that the callback returned null and batch was
		// complete). Is this OK?
		assertEquals(2, count);
		// ... but the interceptor before() was called:
		assertEquals("[1, 2, 1, 2]", calls.toString());
	}

	public void testBeforeInterceptorCanVeto() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.registerListener(new RepeatListenerSupport() {
			public void before(RepeatContext context) {
				calls.add("1");
				context.setCompleteOnly();
			}
		});
		template.iterate(new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				return RepeatStatus.FINISHED;
			}
		});
		assertEquals(0, count);
		// ... but the interceptor before() was called:
		assertEquals("[1]", calls.toString());
	}

	public void testAfterInterceptors() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("1");
			}
		}, new RepeatListenerSupport() {
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("2");
			}
		} });
		template.iterate(new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				return RepeatStatus.continueIf(count <= 1);
			}
		});
		// 2 calls to the callback, and the second one had no processing...
		assertEquals(2, count);
		// ... so the interceptor after() is not called:
		assertEquals("[2, 1]", calls.toString());
	}

	public void testOpenInterceptors() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void open(RepeatContext context) {
				calls.add("1");
			}
		}, new RepeatListenerSupport() {
			public void open(RepeatContext context) {
				calls.add("2");
				context.setCompleteOnly();
			}
		} });
		template.iterate(new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				return RepeatStatus.CONTINUABLE;
			}
		});
		assertEquals(0, count);
		assertEquals("[1, 2]", calls.toString());
	}

	public void testSingleOpenInterceptor() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.registerListener(new RepeatListenerSupport() {
			public void open(RepeatContext context) {
				calls.add("1");
			}
		});
		template.iterate(new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				context.setCompleteOnly();
				return RepeatStatus.FINISHED;
			}
		});
		assertEquals(1, count);
		assertEquals("[1]", calls.toString());
	}

	public void testCloseInterceptors() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void close(RepeatContext context) {
				calls.add("1");
			}
		}, new RepeatListenerSupport() {
			public void close(RepeatContext context) {
				calls.add("2");
			}
		} });
		template.iterate(new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				return RepeatStatus.continueIf(count < 2);
			}
		});
		// Test that more than one call comes in to the callback...
		assertEquals(2, count);
		// ... but the interceptor is only called once.
		assertEquals("[2, 1]", calls.toString());
	}


	public void testOnErrorInterceptors() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void onError(RepeatContext context, Throwable t) {
				calls.add("1");
			}
		}, new RepeatListenerSupport() {
			public void onError(RepeatContext context, Throwable t) {
				calls.add("2");
			}
		} });
		try {
			template.iterate(new RepeatCallback() {
				public RepeatStatus doInIteration(RepeatContext context) throws Exception {
					throw new IllegalStateException("Bogus");
				}
			});
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		assertEquals(0, count);
		assertEquals("[2, 1]", calls.toString());
	}

	public void testOnErrorInterceptorsPrecedence() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<Object>();
		template.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("1");
			}
		}, new RepeatListenerSupport() {
			public void onError(RepeatContext context, Throwable t) {
				calls.add("2");
			}
		} });
		try {
			template.iterate(new RepeatCallback() {
				public RepeatStatus doInIteration(RepeatContext context) throws Exception {
					throw new IllegalStateException("Bogus");
				}
			});
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		assertEquals(0, count);
		// The after is not executed, if there is an error...
		assertEquals("[2]", calls.toString());
	}

	public void testAsynchronousOnErrorInterceptorsPrecedence() throws Exception {
		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		template.setTaskExecutor(new SimpleAsyncTaskExecutor());
		final List<Object> calls = new ArrayList<Object>();
		final List<Object> fails = new ArrayList<Object>();
		template.setListeners(new RepeatListener[] { new RepeatListenerSupport() {
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("1");
			}
		}, new RepeatListenerSupport() {
			public void onError(RepeatContext context, Throwable t) {
				calls.add("2");
				fails.add("2");
			}
		} });
		try {
			template.iterate(new RepeatCallback() {
				public RepeatStatus doInIteration(RepeatContext context) throws Exception {
					throw new IllegalStateException("Bogus");
				}
			});
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
			assertEquals("Bogus", e.getMessage());
		}
		assertEquals(0, count);
		System.err.println(calls);
		// The after is not executed on error...
		assertEquals("2", calls.get(0));
		assertEquals("2", calls.get(calls.size()-1));
		assertFalse(calls.contains("1"));
		assertEquals(fails.size(), calls.size());
	}
}
