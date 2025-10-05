/*
 * Copyright 2006-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.infrastructure.repeat.listener;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.RepeatListener;
import org.springframework.batch.infrastructure.repeat.support.RepeatTemplate;
import org.springframework.batch.infrastructure.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepeatListenerTests {

	private int count = 0;

	@Test
	void testBeforeInterceptors() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.setListeners(new RepeatListener[] { new RepeatListener() {
			@Override
			public void before(RepeatContext context) {
				calls.add("1");
			}
		}, new RepeatListener() {
			@Override
			public void before(RepeatContext context) {
				calls.add("2");
			}
		} });
		template.iterate(context -> {
			count++;
			return RepeatStatus.continueIf(count <= 1);
		});
		// 2 calls: the second time there is no processing
		// (despite the fact that the callback returned null and batch was
		// complete). Is this OK?
		assertEquals(2, count);
		// ... but the interceptor before() was called:
		assertEquals("[1, 2, 1, 2]", calls.toString());
	}

	@Test
	void testBeforeInterceptorCanVeto() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.registerListener(new RepeatListener() {
			@Override
			public void before(RepeatContext context) {
				calls.add("1");
				context.setCompleteOnly();
			}
		});
		template.iterate(context -> {
			count++;
			return RepeatStatus.FINISHED;
		});
		assertEquals(0, count);
		// ... but the interceptor before() was called:
		assertEquals("[1]", calls.toString());
	}

	@Test
	void testAfterInterceptors() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.setListeners(new RepeatListener[] { new RepeatListener() {
			@Override
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("1");
			}
		}, new RepeatListener() {
			@Override
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("2");
			}
		} });
		template.iterate(context -> {
			count++;
			return RepeatStatus.continueIf(count <= 1);
		});
		// 2 calls to the callback, and the second one had no processing...
		assertEquals(2, count);
		// ... so the interceptor after() is not called:
		assertEquals("[2, 1]", calls.toString());
	}

	@Test
	void testOpenInterceptors() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.setListeners(new RepeatListener[] { new RepeatListener() {
			@Override
			public void open(RepeatContext context) {
				calls.add("1");
			}
		}, new RepeatListener() {
			@Override
			public void open(RepeatContext context) {
				calls.add("2");
				context.setCompleteOnly();
			}
		} });
		template.iterate(context -> {
			count++;
			return RepeatStatus.CONTINUABLE;
		});
		assertEquals(0, count);
		assertEquals("[1, 2]", calls.toString());
	}

	@Test
	void testSingleOpenInterceptor() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.registerListener(new RepeatListener() {
			@Override
			public void open(RepeatContext context) {
				calls.add("1");
			}
		});
		template.iterate(context -> {
			count++;
			context.setCompleteOnly();
			return RepeatStatus.FINISHED;
		});
		assertEquals(1, count);
		assertEquals("[1]", calls.toString());
	}

	@Test
	void testCloseInterceptors() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.setListeners(new RepeatListener[] { new RepeatListener() {
			@Override
			public void close(RepeatContext context) {
				calls.add("1");
			}
		}, new RepeatListener() {
			@Override
			public void close(RepeatContext context) {
				calls.add("2");
			}
		} });
		template.iterate(context -> {
			count++;
			return RepeatStatus.continueIf(count < 2);
		});
		// Test that more than one call comes in to the callback...
		assertEquals(2, count);
		// ... but the interceptor is only called once.
		assertEquals("[2, 1]", calls.toString());
	}

	@Test
	void testOnErrorInterceptors() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.setListeners(new RepeatListener[] { new RepeatListener() {
			@Override
			public void onError(RepeatContext context, Throwable t) {
				calls.add("1");
			}
		}, new RepeatListener() {
			@Override
			public void onError(RepeatContext context, Throwable t) {
				calls.add("2");
			}
		} });
		assertThrows(IllegalStateException.class, () -> template.iterate(context -> {
			throw new IllegalStateException("Bogus");
		}));
		assertEquals(0, count);
		assertEquals("[2, 1]", calls.toString());
	}

	@Test
	void testOnErrorInterceptorsPrecedence() {
		RepeatTemplate template = new RepeatTemplate();
		final List<Object> calls = new ArrayList<>();
		template.setListeners(new RepeatListener[] { new RepeatListener() {
			@Override
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("1");
			}
		}, new RepeatListener() {
			@Override
			public void onError(RepeatContext context, Throwable t) {
				calls.add("2");
			}
		} });
		assertThrows(IllegalStateException.class, () -> template.iterate(context -> {
			throw new IllegalStateException("Bogus");
		}));
		assertEquals(0, count);
		// The after is not executed, if there is an error...
		assertEquals("[2]", calls.toString());
	}

	@Test
	void testAsynchronousOnErrorInterceptorsPrecedence() {
		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		template.setTaskExecutor(new SimpleAsyncTaskExecutor());
		final List<Object> calls = new ArrayList<>();
		final List<Object> fails = new ArrayList<>();
		template.setListeners(new RepeatListener[] { new RepeatListener() {
			@Override
			public void after(RepeatContext context, RepeatStatus result) {
				calls.add("1");
			}
		}, new RepeatListener() {
			@Override
			public void onError(RepeatContext context, Throwable t) {
				calls.add("2");
				fails.add("2");
			}
		} });
		Exception exception = assertThrows(IllegalStateException.class, () -> template.iterate(context -> {
			throw new IllegalStateException("Bogus");
		}));
		assertEquals("Bogus", exception.getMessage());
		assertEquals(0, count);
		// The after is not executed on error...
		assertEquals("2", calls.get(0));
		assertEquals("2", calls.get(calls.size() - 1));
		assertFalse(calls.contains("1"));
		assertEquals(fails.size(), calls.size());
	}

}
