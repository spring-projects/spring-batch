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

package org.springframework.batch.repeat.aop;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.exception.RepeatException;

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

	public void testSetTemplate() throws Exception {
		final List calls = new ArrayList();
		interceptor.setRepeatOperations(new RepeatOperations() {
			public ExitStatus iterate(RepeatCallback callback) {
				Object result = "1";
				calls.add(result);
				return ExitStatus.CONTINUABLE;
			}
		});
		((Advised) service).addAdvice(interceptor);
		service.service();
		assertEquals(1, calls.size());
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
			fail("Expected BatchException");
		}
		catch (RepeatException e) {
			assertEquals("Unexpected", e.getMessage().substring(0, 10));
		}
	}

	private interface Service {
		Object service() throws Exception;

		Object exception() throws Exception;

		Object error() throws Exception;
	}

	private static class ServiceImpl implements Service {
		private int count = 0;

		public Object service() throws Exception {
			count++;
			if (count <= 2) {
				return new Integer(count);
			}
			else {
				return null;
			}
		}

		public Object exception() throws Exception {
			throw new RuntimeException("Duh! Stupid.");
		}

		public Object error() throws Exception {
			throw new Error("Duh! Stupid.");
		}
	}
}
