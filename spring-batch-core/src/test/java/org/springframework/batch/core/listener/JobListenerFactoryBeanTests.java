/*
 * Copyright 2002-2025 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
class JobListenerFactoryBeanTests {

	JobListenerFactoryBean factoryBean;

	@BeforeEach
	void setUp() {
		factoryBean = new JobListenerFactoryBean();
	}

	@Test
	void testWithInterface() {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		JobExecution jobExecution = new JobExecution(11L, mock(), mock());
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		assertTrue(delegate.beforeJobCalled);
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	void testWithAnnotations() {
		AnnotatedTestClass delegate = new AnnotatedTestClass();
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		JobExecution jobExecution = new JobExecution(11L, mock(), mock());
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		assertTrue(delegate.beforeJobCalled);
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	void testFactoryMethod() {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener = JobListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof JobExecutionListener);
		((JobExecutionListener) listener).afterJob(new JobExecution(11L, mock(), mock()));
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	void testVanillaInterfaceWithProxy() {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		ProxyFactory factory = new ProxyFactory(delegate);
		factoryBean.setDelegate(factory.getProxy());
		Object listener = factoryBean.getObject();
		assertTrue(listener instanceof JobExecutionListener);
	}

	@Test
	void testUseInHashSet() {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener = JobListenerFactoryBean.getListener(delegate);
		Object other = JobListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof JobExecutionListener);
		Set<JobExecutionListener> listeners = new HashSet<>();
		listeners.add((JobExecutionListener) listener);
		listeners.add((JobExecutionListener) other);
		assertTrue(listeners.contains(listener));
		assertEquals(1, listeners.size());
	}

	@Test
	void testAnnotationsIsListener() {
		assertTrue(JobListenerFactoryBean.isListener(new Object() {
			@BeforeJob
			public void foo(JobExecution execution) {
			}
		}));
	}

	@Test
	void testInterfaceIsListener() {
		assertTrue(JobListenerFactoryBean.isListener(new JobListenerWithInterface()));
	}

	@Test
	void testAnnotationsWithOrdered() {
		Object delegate = new Ordered() {
			@BeforeJob
			public void foo(JobExecution execution) {
			}

			@Override
			public int getOrder() {
				return 3;
			}
		};
		JobExecutionListener listener = JobListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof Ordered, "Listener is not of correct type");
		assertEquals(3, ((Ordered) listener).getOrder());
	}

	@Test
	void testEqualityOfProxies() {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener1 = JobListenerFactoryBean.getListener(delegate);
		Object listener2 = JobListenerFactoryBean.getListener(delegate);
		assertEquals(listener1, listener2);
	}

	private static class JobListenerWithInterface implements JobExecutionListener {

		boolean beforeJobCalled = false;

		boolean afterJobCalled = false;

		@Override
		public void afterJob(JobExecution jobExecution) {
			afterJobCalled = true;
		}

		@Override
		public void beforeJob(JobExecution jobExecution) {
			beforeJobCalled = true;
		}

	}

	private static class AnnotatedTestClass {

		boolean beforeJobCalled = false;

		boolean afterJobCalled = false;

		@BeforeJob
		public void before() {
			beforeJobCalled = true;
		}

		@AfterJob
		public void after() {
			afterJobCalled = true;
		}

	}

}
