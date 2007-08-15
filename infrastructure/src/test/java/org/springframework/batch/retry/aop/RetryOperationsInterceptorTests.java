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

package org.springframework.batch.retry.aop;

import junit.framework.TestCase;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;

public class RetryOperationsInterceptorTests extends TestCase {

	private RetryOperationsInterceptor interceptor;

	private Service service;

	private ServiceImpl target;

	protected void setUp() throws Exception {
		super.setUp();
		interceptor = new RetryOperationsInterceptor();
		target = new ServiceImpl();
		service = (Service) ProxyFactory.getProxy(Service.class, new SingletonTargetSource(target));
	}

	public void testDefaultInterceptorSunnyDay() throws Exception {
		((Advised) service).addAdvice(interceptor);
		service.service();
		assertEquals(2, target.count);
	}

	public void testRetryExceptionAfterTooManyAttempts() throws Exception {
		((Advised) service).addAdvice(interceptor);
		RetryTemplate template = new RetryTemplate();
		template.setRetryPolicy(new NeverRetryPolicy());
		interceptor.setRetryTemplate(template);
		try {
			service.service();
			fail("Expected Exception.");
		}
		catch (Exception e) {
			assertTrue(e.getMessage().startsWith("Not enough calls"));
		}
		assertEquals(1, target.count);
	}

	private interface Service {
		void service() throws Exception;
	}

	private static class ServiceImpl implements Service {
		private int count = 0;

		public void service() throws Exception {
			count++;
			if (count < 2) {
				throw new Exception("Not enough calls: " + count);
			}
		}
	}
}
