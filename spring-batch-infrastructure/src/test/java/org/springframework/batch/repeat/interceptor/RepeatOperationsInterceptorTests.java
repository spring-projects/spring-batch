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

package org.springframework.batch.repeat.interceptor;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;

public class RepeatOperationsInterceptorTests extends TestCase {

	private RepeatOperationsInterceptor interceptor;

	private Service service;

	private ServiceImpl target;

	protected void setUp() throws Exception {
		super.setUp();
		interceptor = new RepeatOperationsInterceptor();
		target = new ServiceImpl();
		ProxyFactory factory = new ProxyFactory(RepeatOperations.class.getClassLoader());
		factory.setInterfaces(new Class[] { Service.class });
		factory.setTarget(target);
		service = (Service) factory.getProxy();
	}

	public void testDefaultInterceptorSunnyDay() throws Exception {
		((Advised) service).addAdvice(interceptor);
		service.service();
		assertEquals(3, target.count);
	}

	public void testCompleteOnFirstInvocation() throws Exception {
		((Advised) service).addAdvice(interceptor);
		target.setMaxService(0);
		service.service();
		assertEquals(1, target.count);
	}

	public void testSetTemplate() throws Exception {
		final List<Object> calls = new ArrayList<Object>();
		interceptor.setRepeatOperations(new RepeatOperations() {
			public RepeatStatus iterate(RepeatCallback callback) {
				try {
					Object result = callback.doInIteration(null);
					calls.add(result);
				}
				catch (Exception e) {
					throw new RepeatException("Encountered exception in repeat.", e);
				}
				return RepeatStatus.CONTINUABLE;
			}
		});
		((Advised) service).addAdvice(interceptor);
		service.service();
		assertEquals(1, calls.size());
	}

	public void testCallbackNotExecuted() throws Exception {
		final List<Object> calls = new ArrayList<Object>();
		interceptor.setRepeatOperations(new RepeatOperations() {
			public RepeatStatus iterate(RepeatCallback callback) {
				calls.add(null);
				return RepeatStatus.FINISHED;
			}
		});
		((Advised) service).addAdvice(interceptor);
		try {
			service.service();
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			String message = e.getMessage();
			assertTrue("Wrong exception message: "+message, message.toLowerCase().indexOf("no result available")>=0);
		}
		assertEquals(1, calls.size());
	}

	public void testVoidServiceSunnyDay() throws Exception {
		((Advised) service).addAdvice(interceptor);
		RepeatTemplate template = new RepeatTemplate();
		// N.B. the default completion policy results in an infinite loop, so we
		// need to set the chunk size.
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		interceptor.setRepeatOperations(template);
		service.alternate();
		assertEquals(2, target.count);
	}

	public void testCallbackWithException() throws Exception {
		((Advised) service).addAdvice(interceptor);
		try {
			service.exception();
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Duh", e.getMessage().substring(0, 3));
		}
	}

	public void testCallbackWithThrowable() throws Exception {
		((Advised) service).addAdvice(interceptor);
		try {
			service.error();
			fail("Expected Error");
		}
		catch (Error e) {
			assertEquals("Duh", e.getMessage().substring(0, 3));
		}
	}

	public void testCallbackWithBoolean() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		// N.B. the default completion policy results in an infinite loop, so we
		// need to set the chunk size.
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		interceptor.setRepeatOperations(template);
		((Advised) service).addAdvice(interceptor);
		assertTrue(service.isContinuable());
		assertEquals(2, target.count);
	}

	public void testCallbackWithBooleanReturningFalseFirstTime() throws Exception {
		target.setComplete(true);
		((Advised) service).addAdvice(interceptor);
		// Complete without repeat when boolean return value is false
		assertFalse(service.isContinuable());
		assertEquals(1, target.count);
	}

	public void testInterceptorChainWithRetry() throws Exception {
		((Advised) service).addAdvice(interceptor);
		final List<Object> list = new ArrayList<Object>();
		((Advised) service).addAdvice(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				list.add("chain");
				return invocation.proceed();
			}
		});
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		interceptor.setRepeatOperations(template);
		service.service();
		assertEquals(2, target.count);
		assertEquals(2, list.size());
	}

	public void testIllegalMethodInvocationType() throws Throwable {
		try {
			interceptor.invoke(new MethodInvocation() {
				public Method getMethod() {
					try {
						return Object.class.getMethod("toString", new Class[0]);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				public Object[] getArguments() {
					return null;
				}

				public AccessibleObject getStaticPart() {
					return null;
				}

				public Object getThis() {
					return null;
				}

				public Object proceed() throws Throwable {
					return null;
				}
			});
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e) {
			assertTrue("Exception message should contain MethodInvocation: " + e.getMessage(), e.getMessage().indexOf(
					"MethodInvocation") >= 0);
		}
	}

	private interface Service {
		Object service() throws Exception;

		void alternate() throws Exception;

		Object exception() throws Exception;

		Object error() throws Exception;

		boolean isContinuable() throws Exception;
	}

	private static class ServiceImpl implements Service {
		private int count = 0;

		private boolean complete;

		private int maxService = 2;
		
		/**
		 * Public setter for the maximum number of times to call service().
		 * @param maxService the maxService to set
		 */
		public void setMaxService(int maxService) {
			this.maxService = maxService;
		}

		public Object service() throws Exception {
			count++;
			if (count <= maxService) {
				return Integer.valueOf(count);
			}
			else {
				return null;
			}
		}

		public void setComplete(boolean complete) {
			this.complete = complete;
		}

		public void alternate() throws Exception {
			count++;
		}

		public Object exception() throws Exception {
			throw new RuntimeException("Duh! Stupid.");
		}

		public Object error() throws Exception {
			throw new Error("Duh! Stupid error.");
		}

		public boolean isContinuable() throws Exception {
			count++;
			return !complete;
		}
	}

}
