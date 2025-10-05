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

package org.springframework.batch.infrastructure.repeat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.RepeatException;
import org.springframework.batch.infrastructure.repeat.RepeatListener;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.callback.NestedRepeatCallback;
import org.springframework.batch.infrastructure.repeat.context.RepeatContextSupport;
import org.springframework.batch.infrastructure.repeat.exception.ExceptionHandler;
import org.springframework.batch.infrastructure.repeat.policy.CompletionPolicySupport;
import org.springframework.batch.infrastructure.repeat.policy.SimpleCompletionPolicy;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
class SimpleRepeatTemplateTests extends AbstractTradeBatchTests {

	private RepeatTemplate template = getRepeatTemplate();

	private int count = 0;

	protected RepeatTemplate getRepeatTemplate() {
		template = new RepeatTemplate();
		// default stop after more items than exist in dataset
		template.setCompletionPolicy(new SimpleCompletionPolicy(8));
		return template;
	}

	@Test
	void testExecute() {
		template.iterate(new ItemReaderRepeatCallback<>(provider, processor));
		assertEquals(NUMBER_OF_ITEMS, processor.count);
	}

	/**
	 * Check that a dedicated TerminationPolicy can terminate the batch.
	 */
	@Test
	void testEarlyCompletionWithPolicy() {

		template.setCompletionPolicy(new SimpleCompletionPolicy(2));

		template.iterate(new ItemReaderRepeatCallback<>(provider, processor));

		assertEquals(2, processor.count);

	}

	/**
	 * Check that a dedicated TerminationPolicy can terminate the batch.
	 */
	@Test
	void testEarlyCompletionWithException() {

		Exception exception = assertThrows(IllegalStateException.class, () -> template.iterate(context -> {
			count++;
			throw new IllegalStateException("foo!");
		}));
		assertEquals("foo!", exception.getMessage());

		assertEquals(1, count);
		assertTrue(count <= 10, "Too many attempts: " + count);

	}

	/**
	 * Check that the context is closed.
	 */
	@Test
	void testContextClosedOnNormalCompletion() {

		final List<String> list = new ArrayList<>();

		final RepeatContext context = new RepeatContextSupport(null) {
			@Override
			public void close() {
				super.close();
				list.add("close");
			}
		};
		template.setCompletionPolicy(new CompletionPolicySupport() {
			@Override
			public RepeatContext start(RepeatContext c) {
				return context;
			}
		});
		template.iterate(context1 -> {
			count++;
			return RepeatStatus.continueIf(count < 1);
		});

		assertEquals(1, count);
		assertEquals(1, list.size());

	}

	/**
	 * Check that the context is closed.
	 */
	@Test
	void testContextClosedOnAbnormalCompletion() {

		final List<String> list = new ArrayList<>();

		final RepeatContext context = new RepeatContextSupport(null) {
			@Override
			public void close() {
				super.close();
				list.add("close");
			}
		};
		template.setCompletionPolicy(new CompletionPolicySupport() {
			@Override
			public RepeatContext start(RepeatContext c) {
				return context;
			}
		});

		Exception exception = assertThrows(RuntimeException.class, () -> template.iterate(context1 -> {
			count++;
			throw new RuntimeException("foo");
		}));
		assertEquals("foo", exception.getMessage());

		assertEquals(1, count);
		assertEquals(1, list.size());

	}

	/**
	 * Check that the exception handler is called.
	 */
	@Test
	void testExceptionHandlerCalledOnAbnormalCompletion() {

		final List<Throwable> list = new ArrayList<>();

		template.setExceptionHandler((context, throwable) -> {
			list.add(throwable);
			throw throwable;
		});

		Exception exception = assertThrows(RuntimeException.class, () -> template.iterate(context -> {
			count++;
			throw new RuntimeException("foo");
		}));
		assertEquals("foo", exception.getMessage());

		assertEquals(1, count);
		assertEquals(1, list.size());

	}

	/**
	 * Check that a the context can be used to signal early completion.
	 */
	@Test
	void testEarlyCompletionWithContext() {

		RepeatStatus result = template.iterate(new ItemReaderRepeatCallback<>(provider, processor) {

			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				RepeatStatus result = super.doInIteration(context);
				if (processor.count >= 2) {
					context.setCompleteOnly();
					// If we return null the batch will terminate anyway
					// without an exception...
				}
				return result;
			}
		});

		// 2 items were processed before completion signalled
		assertEquals(2, processor.count);

		// Not all items processed
		assertTrue(result.isContinuable());

	}

	/**
	 * Check that a the context can be used to signal early completion.
	 */
	@Test
	void testEarlyCompletionWithContextTerminated() {

		RepeatStatus result = template.iterate(new ItemReaderRepeatCallback<>(provider, processor) {

			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				RepeatStatus result = super.doInIteration(context);
				if (processor.count >= 2) {
					context.setTerminateOnly();
					// If we return null the batch will terminate anyway
					// without an exception...
				}
				return result;
			}
		});

		// 2 items were processed before completion signalled
		assertEquals(2, processor.count);

		// Not all items processed
		assertTrue(result.isContinuable());

	}

	@Test
	void testNestedSession() {
		RepeatTemplate outer = getRepeatTemplate();
		RepeatTemplate inner = getRepeatTemplate();
		outer.iterate(new NestedRepeatCallback(inner, context -> {
			count++;
			assertNotNull(context);
			assertNotSame(context, context.getParent(), "Nested batch should have new session");
			assertSame(context, RepeatSynchronizationManager.getContext());
			return RepeatStatus.FINISHED;
		}) {
			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				assertSame(context, RepeatSynchronizationManager.getContext());
				return super.doInIteration(context);
			}
		});
		assertEquals(2, count);
	}

	@Test
	void testNestedSessionTerminatesBeforeIteration() {
		RepeatTemplate outer = getRepeatTemplate();
		RepeatTemplate inner = getRepeatTemplate();
		outer.iterate(new NestedRepeatCallback(inner, context -> {
			count++;
			assertEquals(2, count);
			fail("Nested batch should not have been executed");
			return RepeatStatus.FINISHED;
		}) {
			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				context.setCompleteOnly();
				return super.doInIteration(context);
			}
		});
		assertEquals(1, count);
	}

	@Test
	void testOuterContextPreserved() {
		RepeatTemplate outer = getRepeatTemplate();
		outer.setCompletionPolicy(new SimpleCompletionPolicy(2));
		RepeatTemplate inner = getRepeatTemplate();
		outer.iterate(new NestedRepeatCallback(inner, context -> {
			count++;
			assertNotNull(context);
			assertNotSame(context, context.getParent(), "Nested batch should have new session");
			assertSame(context, RepeatSynchronizationManager.getContext());
			return RepeatStatus.FINISHED;
		}) {
			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				assertSame(context, RepeatSynchronizationManager.getContext());
				super.doInIteration(context);
				return RepeatStatus.CONTINUABLE;
			}
		});
		assertEquals(4, count);
	}

	/**
	 * Test that a result is returned from the batch.
	 */
	@Test
	void testResult() {
		RepeatStatus result = template.iterate(new ItemReaderRepeatCallback<>(provider, processor));
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		// We are complete - do not expect to be called again
		assertFalse(result.isContinuable());
	}

	@Test
	void testExceptionThrownOnLastItem() {
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		Exception exception = assertThrows(Exception.class, () -> template.iterate(context -> {
			count++;
			if (count < 2) {
				return RepeatStatus.CONTINUABLE;
			}
			throw new RuntimeException("Barf second try count=" + count);
		}));
		assertEquals("Barf second try count=2", exception.getMessage());
	}

	/**
	 * Check that a the session can be used to signal early completion, but an exception
	 * takes precedence.
	 */
	@Test
	void testEarlyCompletionWithSessionAndException() {

		template.setCompletionPolicy(new SimpleCompletionPolicy(4));

		RepeatStatus result = RepeatStatus.FINISHED;

		Exception exception = assertThrows(RuntimeException.class,
				() -> template.iterate(new ItemReaderRepeatCallback<>(provider, processor) {
					@Override
					public RepeatStatus doInIteration(RepeatContext context) throws Exception {
						RepeatStatus result = super.doInIteration(context);
						if (processor.count >= 2) {
							context.setCompleteOnly();
							throw new RuntimeException("Barf second try count=" + processor.count);
						}
						return result;
					}
				}));
		assertEquals("Barf second try count=2", exception.getMessage());

		// 2 items were processed before completion signalled
		assertEquals(2, processor.count);

		// An exception was thrown by the template so result is still false
		assertFalse(result.isContinuable());

	}

	/**
	 * Checked exceptions are wrapped into runtime RepeatException. RepeatException should
	 * be unwrapped before it is passed to listeners and exception handler.
	 */
	@Test
	void testExceptionUnwrapping() {

		class TestException extends Exception {

			TestException(String msg) {
				super(msg);
			}

		}
		final TestException exception = new TestException("CRASH!");

		class ExceptionHandlerStub implements ExceptionHandler {

			boolean called = false;

			@Override
			public void handleException(RepeatContext context, Throwable throwable) throws Throwable {
				called = true;
				assertSame(exception, throwable);
				throw throwable; // re-throw so that repeat template
				// terminates iteration
			}

		}
		ExceptionHandlerStub exHandler = new ExceptionHandlerStub();

		class RepeatListenerStub implements RepeatListener {

			boolean called = false;

			@Override
			public void onError(RepeatContext context, Throwable throwable) {
				called = true;
				assertSame(exception, throwable);
			}

		}
		RepeatListenerStub listener = new RepeatListenerStub();

		template.setExceptionHandler(exHandler);
		template.setListeners(new RepeatListener[] { listener });

		Exception expected = assertThrows(RepeatException.class, () -> template.iterate(context -> {
			throw new RepeatException("typically thrown by nested repeat template", exception);
		}));
		assertSame(exception, expected.getCause());

		assertTrue(listener.called);
		assertTrue(exHandler.called);

	}

}
