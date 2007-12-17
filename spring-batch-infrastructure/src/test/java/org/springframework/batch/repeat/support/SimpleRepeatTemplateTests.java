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

package org.springframework.batch.repeat.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.callback.ItemReaderRepeatCallback;
import org.springframework.batch.repeat.callback.NestedRepeatCallback;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.policy.CompletionPolicySupport;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;

/**
 * @author Dave Syer
 */
public class SimpleRepeatTemplateTests extends AbstractTradeBatchTests {

	RepeatTemplate template = getRepeatTemplate();

	int count = 0;

	public RepeatTemplate getRepeatTemplate() {
		return new RepeatTemplate();
	}

	public void testExecute() throws Exception {
		template.iterate(new ItemReaderRepeatCallback(provider, processor));
		assertEquals(NUMBER_OF_ITEMS, processor.count);
	}

	/**
	 * Check that a dedicated TerminationPolicy can terminate the batch.
	 * 
	 * @throws Exception
	 */
	public void testEarlyCompletionWithPolicy() throws Exception {

		template.setCompletionPolicy(new SimpleCompletionPolicy(2));

		template.iterate(new ItemReaderRepeatCallback(provider, processor));

		assertEquals(2, processor.count);

	}

	/**
	 * Check that a dedicated TerminationPolicy can terminate the batch.
	 * 
	 * @throws Exception
	 */
	public void testEarlyCompletionWithException() throws Exception {

		try {
			template.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					count++;
					throw new IllegalStateException("foo!");
				}
			});
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			assertEquals("foo!", e.getMessage());
		}

		assertEquals(1, count);

	}

	/**
	 * Check that the context is closed.
	 * 
	 * @throws Exception
	 */
	public void testContextClosedOnNormalCompletion() throws Exception {

		final List list = new ArrayList();

		final RepeatContext context = new RepeatContextSupport(null) {
			public void close() {
				super.close();
				list.add("close");
			}
		};
		template.setCompletionPolicy(new CompletionPolicySupport() {
			public RepeatContext start(RepeatContext c) {
				return context;
			}
		});
		template.iterate(new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				return new ExitStatus(count < 1);
			}
		});

		assertEquals(1, count);
		assertEquals(1, list.size());

	}

	/**
	 * Check that the context is closed.
	 * 
	 * @throws Exception
	 */
	public void testContextClosedOnAbnormalCompletion() throws Exception {

		final List list = new ArrayList();

		final RepeatContext context = new RepeatContextSupport(null) {
			public void close() {
				super.close();
				list.add("close");
			}
		};
		template.setCompletionPolicy(new CompletionPolicySupport() {
			public RepeatContext start(RepeatContext c) {
				return context;
			}
		});

		try {
			template.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					count++;
					throw new RuntimeException("foo");
				}
			});
		}
		catch (RuntimeException e) {
			assertEquals("foo", e.getMessage());
		}

		assertEquals(1, count);
		assertEquals(1, list.size());

	}

	/**
	 * Check that the exception handler is called.
	 * 
	 * @throws Exception
	 */
	public void testExceptionHandlerCalledOnAbnormalCompletion() throws Exception {

		final List list = new ArrayList();

		template.setExceptionHandler(new ExceptionHandler() {
			public void handleException(RepeatContext context, Throwable throwable) throws RuntimeException {
				list.add(throwable);
				throw (RuntimeException) throwable;
			}
		});

		try {
			template.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					count++;
					throw new RuntimeException("foo");
				}
			});
		}
		catch (RuntimeException e) {
			assertEquals("foo", e.getMessage());
		}

		assertEquals(1, count);
		assertEquals(1, list.size());

	}

	/**
	 * Check that a the context can be used to signal early completion.
	 * 
	 * @throws Exception
	 */
	public void testEarlyCompletionWithContext() throws Exception {

		ExitStatus result = template.iterate(new ItemReaderRepeatCallback(provider, processor) {

			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				ExitStatus result = super.doInIteration(context);
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
	 * 
	 * @throws Exception
	 */
	public void testEarlyCompletionWithContextTerminated() throws Exception {

		ExitStatus result = template.iterate(new ItemReaderRepeatCallback(provider, processor) {

			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				ExitStatus result = super.doInIteration(context);
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

	public void testNestedSession() throws Exception {
		RepeatTemplate outer = getRepeatTemplate();
		RepeatTemplate inner = getRepeatTemplate();
		outer.iterate(new NestedRepeatCallback(inner, new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				assertNotNull(context);
				assertNotSame("Nested batch should have new session", context, context.getParent());
				assertSame(context, RepeatSynchronizationManager.getContext());
				return ExitStatus.FINISHED;
			}
		}) {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				// TODO parameter is rewritten and then compared to value it has
				// just been assigned
				context = RepeatSynchronizationManager.getContext();
				assertSame(context, RepeatSynchronizationManager.getContext());
				return super.doInIteration(context);
			}
		});
		assertEquals(2, count);
	}

	public void testNestedSessionTerminatesBeforeIteration() throws Exception {
		RepeatTemplate outer = getRepeatTemplate();
		RepeatTemplate inner = getRepeatTemplate();
		outer.iterate(new NestedRepeatCallback(inner, new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				assertEquals(2, count);
				fail("Nested batch should not have been executed");
				return ExitStatus.FINISHED;
			}
		}) {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				context.setCompleteOnly();
				return super.doInIteration(context);
			}
		});
		assertEquals(1, count);
	}

	public void testOuterContextPreserved() throws Exception {
		RepeatTemplate outer = getRepeatTemplate();
		outer.setCompletionPolicy(new SimpleCompletionPolicy(2));
		RepeatTemplate inner = getRepeatTemplate();
		outer.iterate(new NestedRepeatCallback(inner, new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				assertNotNull(context);
				assertNotSame("Nested batch should have new session", context, context.getParent());
				assertSame(context, RepeatSynchronizationManager.getContext());
				return ExitStatus.FINISHED;
			}
		}) {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				// TODO parameter is rewritten and then compared to value it has
				// just been assigned
				context = RepeatSynchronizationManager.getContext();
				assertSame(context, RepeatSynchronizationManager.getContext());
				super.doInIteration(context);
				return ExitStatus.CONTINUABLE;
			}
		});
		assertEquals(4, count);
	}

	/**
	 * Test that a result is returned from the batch.
	 * @throws Exception
	 */
	public void testResult() throws Exception {
		ExitStatus result = template.iterate(new ItemReaderRepeatCallback(provider, processor));
		assertEquals(NUMBER_OF_ITEMS, processor.count);
		// We are complete - do not expect to be called again
		assertFalse(result.isContinuable());
	}

	public void testExceptionThrownOnLastItem() throws Exception {
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		try {
			template.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					count++;
					if (count < 2) {
						return ExitStatus.CONTINUABLE;
					}
					throw new RuntimeException("Barf second try count=" + count);
				}
			});
			fail("Expected exception on last item in batch");
		}
		catch (Exception e) {
			// expected
			assertEquals("Barf second try count=2", e.getMessage());
		}
	}

	/**
	 * Check that a the session can be used to signal early completion, but an
	 * exception takes precedence.
	 * 
	 * @throws Exception
	 */
	public void testEarlyCompletionWithSessionAndException() throws Exception {

		template.setCompletionPolicy(new SimpleCompletionPolicy(4));

		ExitStatus result = ExitStatus.FINISHED;

		try {
			result = template.iterate(new ItemReaderRepeatCallback(provider, processor) {

				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					ExitStatus result = super.doInIteration(context);
					if (processor.count >= 2) {
						context.setCompleteOnly();
						throw new RuntimeException("Barf second try count=" + processor.count);
					}
					return result;
				}
			});
			fail("Expected exception on last item in batch");
		}
		catch (RuntimeException e) {
			// expected
			assertEquals("Barf second try count=2", e.getMessage());
		}

		// 2 items were processed before completion signalled
		assertEquals(2, processor.count);

		System.err.println(result);

		// An exception was thrown by the template so result is still false
		assertFalse(result.isContinuable());

	}
	
	public void testCustomExitCode(){
		
		ExitStatus status = template.iterate(new RepeatCallback(){

			public ExitStatus doInIteration(RepeatContext context)
					throws Exception {
				ExitStatus exitStatus = new ExitStatus(false, "CUSTOM_CODE");
				return exitStatus;
			}
			
		});
		
		assertEquals("CUSTOM_CODE", status.getExitCode());
	}

}
