/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.batch.core.listener;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.springframework.batch.core.listener.JobListenerMetaData.AFTER_JOB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.configuration.xml.AbstractTestComponent;
import org.springframework.core.Ordered;

/**
 * @author Lucas Ward
 * 
 */
public class JobListenerFactoryBeanTests {

	JobListenerFactoryBean factoryBean;

	@Before
	public void setUp() {
		factoryBean = new JobListenerFactoryBean();
	}

	@Test
	public void testWithInterface() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		JobExecution jobExecution = new JobExecution(11L);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		assertTrue(delegate.beforeJobCalled);
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	public void testWithAnnotations() throws Exception {
		AnnotatedTestClass delegate = new AnnotatedTestClass();
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		JobExecution jobExecution = new JobExecution(11L);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		assertTrue(delegate.beforeJobCalled);
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	public void testFactoryMethod() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener = JobListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof JobExecutionListener);
		((JobExecutionListener) listener).afterJob(new JobExecution(11L));
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	public void testVanillaInterfaceWithProxy() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		ProxyFactory factory = new ProxyFactory(delegate);
		factoryBean.setDelegate(factory.getProxy());
		Object listener = factoryBean.getObject();
		assertTrue(listener instanceof JobExecutionListener);
	}

	@Test
	public void testUseInHashSet() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener = JobListenerFactoryBean.getListener(delegate);
		Object other = JobListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof JobExecutionListener);
		Set<JobExecutionListener> listeners = new HashSet<JobExecutionListener>();
		listeners.add((JobExecutionListener) listener);
		listeners.add((JobExecutionListener) other);
		assertTrue(listeners.contains(listener));
		assertEquals(1, listeners.size());
	}

	@Test
	public void testAnnotationsIsListener() throws Exception {
		assertTrue(JobListenerFactoryBean.isListener(new Object() {
			@SuppressWarnings("unused")
			@BeforeJob
			public void foo(JobExecution execution) {
			}
		}));
	}

	@Test
	public void testInterfaceIsListener() throws Exception {
		assertTrue(JobListenerFactoryBean.isListener(new JobListenerWithInterface()));
	}

	@Test
	public void testAnnotationsWithOrdered() throws Exception {
		Object delegate = new Ordered() {
			@SuppressWarnings("unused")
			@BeforeJob
			public void foo(JobExecution execution) {
			}

			public int getOrder() {
				return 3;
			}
		};
		JobExecutionListener listener = JobListenerFactoryBean.getListener(delegate);
		assertTrue("Listener is not of correct type", listener instanceof Ordered);
		assertEquals(3, ((Ordered) listener).getOrder());
	}

	@Test
	public void testEqualityOfProxies() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener1 = JobListenerFactoryBean.getListener(delegate);
		Object listener2 = JobListenerFactoryBean.getListener(delegate);
		assertEquals(listener1, listener2);
	}

	@Test
	public void testEmptySignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			@AfterJob
			public void aMethod() {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		listener.afterJob(new JobExecution(1L));
		assertTrue(delegate.isExecuted());
	}

	@Test
	public void testRightSignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			@AfterJob
			public void aMethod(JobExecution jobExecution) {
				executed = true;
				assertEquals(new Long(25), jobExecution.getId());
			}
		};
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		listener.afterJob(new JobExecution(25L));
		assertTrue(delegate.isExecuted());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongSignatureAnnotation() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			@AfterJob
			public void aMethod(Integer item) {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		factoryBean.getObject();
	}

	@Test
	public void testEmptySignatureNamedMethod() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			public void aMethod() {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<String, String>();
		metaDataMap.put(AFTER_JOB.getPropertyName(), "aMethod");
		factoryBean.setMetaDataMap(metaDataMap);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		listener.afterJob(new JobExecution(1L));
		assertTrue(delegate.isExecuted());
	}

	@Test
	public void testRightSignatureNamedMethod() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			public void aMethod(JobExecution jobExecution) {
				executed = true;
				assertEquals(new Long(25), jobExecution.getId());
			}
		};
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<String, String>();
		metaDataMap.put(AFTER_JOB.getPropertyName(), "aMethod");
		factoryBean.setMetaDataMap(metaDataMap);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		listener.afterJob(new JobExecution(25L));
		assertTrue(delegate.isExecuted());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongSignatureNamedMethod() {
		AbstractTestComponent delegate = new AbstractTestComponent() {
			@SuppressWarnings("unused")
			public void aMethod(Integer item) {
				executed = true;
			}
		};
		factoryBean.setDelegate(delegate);
		Map<String, String> metaDataMap = new HashMap<String, String>();
		metaDataMap.put(AFTER_JOB.getPropertyName(), "aMethod");
		factoryBean.setMetaDataMap(metaDataMap);
		factoryBean.getObject();
	}

	private class JobListenerWithInterface implements JobExecutionListener {

		boolean beforeJobCalled = false;

		boolean afterJobCalled = false;

		public void afterJob(JobExecution jobExecution) {
			afterJobCalled = true;
		}

		public void beforeJob(JobExecution jobExecution) {
			beforeJobCalled = true;
		}

	}

	@SuppressWarnings("unused")
	private class AnnotatedTestClass {

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
