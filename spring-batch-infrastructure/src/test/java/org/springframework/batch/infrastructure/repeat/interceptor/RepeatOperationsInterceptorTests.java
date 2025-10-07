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

package org.springframework.batch.infrastructure.repeat.interceptor;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.RepeatException;
import org.springframework.batch.infrastructure.repeat.RepeatOperations;
import org.springframework.batch.infrastructure.repeat.interceptor.RepeatOperationsInterceptor;
import org.springframework.batch.infrastructure.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.infrastructure.repeat.support.RepeatTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepeatOperationsInterceptorTests {

	private RepeatOperationsInterceptor interceptor;

	private Service service;

	private ServiceImpl target;

	@BeforeEach
	void setUp() {
		interceptor = new RepeatOperationsInterceptor();
		target = new ServiceImpl();
		ProxyFactory factory = new ProxyFactory(RepeatOperations.class.getClassLoader());
		factory.setInterfaces(Service.class);
		factory.setTarget(target);
		service = (Service) factory.getProxy();
	}

	@Test
	void testDefaultInterceptorSunnyDay() throws Exception {
		((Advised) service).addAdvice(interceptor);
		service.service();
		assertEquals(3, target.count);
	}

	@Test
	void testCompleteOnFirstInvocation() throws Exception {
		((Advised) service).addAdvice(interceptor);
		target.setMaxService(0);
		service.service();
		assertEquals(1, target.count);
	}

	@Test
	void testSetTemplate() throws Exception {
		final List<Object> calls = new ArrayList<>();
		interceptor.setRepeatOperations(callback -> {
			try {
				Object result = callback.doInIteration(null);
				calls.add(result);
			}
			catch (Exception e) {
				throw new RepeatException("Encountered exception in repeat.", e);
			}
			return RepeatStatus.CONTINUABLE;
		});
		((Advised) service).addAdvice(interceptor);
		service.service();
		assertEquals(1, calls.size());
	}

	@Test
	void testCallbackNotExecuted() {
		final List<Object> calls = new ArrayList<>();
		interceptor.setRepeatOperations(callback -> {
			calls.add(null);
			return RepeatStatus.FINISHED;
		});
		((Advised) service).addAdvice(interceptor);
		Exception exception = assertThrows(IllegalStateException.class, service::service);
		String message = exception.getMessage();
		assertTrue(message.toLowerCase().contains("no result available"), "Wrong exception message: " + message);
		assertEquals(1, calls.size());
	}

	@Test
	void testVoidServiceSunnyDay() throws Exception {
		((Advised) service).addAdvice(interceptor);
		RepeatTemplate template = new RepeatTemplate();
		// N.B. the default completion policy results in an infinite loop, so we
		// need to set the chunk size.
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		interceptor.setRepeatOperations(template);
		service.alternate();
		assertEquals(2, target.count);
	}

	@Test
	void testCallbackWithException() {
		((Advised) service).addAdvice(interceptor);
		Exception exception = assertThrows(RuntimeException.class, service::exception);
		assertEquals("Duh", exception.getMessage().substring(0, 3));
	}

	@Test
	void testCallbackWithThrowable() {
		((Advised) service).addAdvice(interceptor);
		Error error = assertThrows(Error.class, service::error);
		assertEquals("Duh", error.getMessage().substring(0, 3));
	}

	@Test
	void testCallbackWithBoolean() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		// N.B. the default completion policy results in an infinite loop, so we
		// need to set the chunk size.
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		interceptor.setRepeatOperations(template);
		((Advised) service).addAdvice(interceptor);
		assertTrue(service.isContinuable());
		assertEquals(2, target.count);
	}

	@Test
	void testCallbackWithBooleanReturningFalseFirstTime() throws Exception {
		target.setComplete(true);
		((Advised) service).addAdvice(interceptor);
		// Complete without repeat when boolean return value is false
		assertFalse(service.isContinuable());
		assertEquals(1, target.count);
	}

	@Test
	void testInterceptorChainWithRetry() throws Exception {
		((Advised) service).addAdvice(interceptor);
		final List<Object> list = new ArrayList<>();
		((Advised) service).addAdvice((MethodInterceptor) invocation -> {
			list.add("chain");
			return invocation.proceed();
		});
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		interceptor.setRepeatOperations(template);
		service.service();
		assertEquals(2, target.count);
		assertEquals(2, list.size());
	}

	@Test
	void testIllegalMethodInvocationType() {
		Exception exception = assertThrows(IllegalStateException.class,
				() -> interceptor.invoke(new MethodInvocation() {
					@Override
					public Method getMethod() {
						try {
							return Object.class.getMethod("toString");
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
					}

					@Override
					public Object[] getArguments() {
						return null;
					}

					@Override
					public AccessibleObject getStaticPart() {
						return null;
					}

					@Override
					public Object getThis() {
						return null;
					}

					@Override
					public Object proceed() throws Throwable {
						return null;
					}
				}));
		String message = exception.getMessage();
		assertTrue(message.contains("MethodInvocation"),
				"Exception message should contain MethodInvocation: " + message);
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

		@Override
		public @Nullable Object service() throws Exception {
			count++;
			if (count <= maxService) {
				return count;
			}
			else {
				return null;
			}
		}

		public void setComplete(boolean complete) {
			this.complete = complete;
		}

		@Override
		public void alternate() throws Exception {
			count++;
		}

		@Override
		public Object exception() throws Exception {
			throw new RuntimeException("Duh! Stupid.");
		}

		@Override
		public Object error() throws Exception {
			throw new Error("Duh! Stupid error.");
		}

		@Override
		public boolean isContinuable() throws Exception {
			count++;
			return !complete;
		}

	}

}
