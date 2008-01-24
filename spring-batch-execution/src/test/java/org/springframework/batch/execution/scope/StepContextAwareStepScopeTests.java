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
package org.springframework.batch.execution.scope;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.AttributeAccessor;

/**
 * @author Dave Syer
 *
 */
public class StepContextAwareStepScopeTests extends TestCase {
	
	private static List list = new ArrayList();
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		StepSynchronizationManager.clear();
		list.clear();
	}
	
	public void testScopedBean() throws Exception {
		StepSynchronizationManager.register(new SimpleStepContext(null));
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		TestBean bean = (TestBean) applicationContext.getBean("bean");
		assertNotNull(bean);
		assertEquals("foo", bean.name);
	}
	
	public void testScopedBeanWithDestroyCallback() throws Exception {
		assertEquals(0, list.size());
		StepSynchronizationManager.register(new SimpleStepContext(null));
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		TestBean bean = (TestBean) applicationContext.getBean("bean");
		assertNotNull(bean);
		StepSynchronizationManager.close();
		assertEquals(1, list.size());
	}

	public void testScopedBeanWithAware() throws Exception {
		StepContext context = new SimpleStepContext(null);
		StepSynchronizationManager.register(context);
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		TestBeanAware bean = (TestBeanAware) applicationContext.getBean("aware");
		assertNotNull(bean);
		assertEquals("bar", bean.name);
		assertEquals(context, bean.context);
	}

	public void testScopedBeanWithInner() throws Exception {
		StepSynchronizationManager.register(new SimpleStepContext(null));
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"scope-tests.xml", getClass());
		TestBean bean = ((TestBean) applicationContext.getBean("inner")).child;
		assertNotNull(bean);
		assertEquals("bar", bean.name);
		StepSynchronizationManager.close();
		assertEquals(1, list.size());
	}

	public void testScopedBeanWithProxy() throws Exception {
		StepContext context = new SimpleStepContext(null);
		StepSynchronizationManager.register(context);
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		TestBeanAware bean = (TestBeanAware) applicationContext.getBean("proxy");
		assertNotNull(bean);
		// A scoped proxy is only accessible through public methods
		assertEquals(null, bean.name);
		assertEquals("spam", bean.getName());
		assertEquals(context, bean.getContext());
	}

	public void testScopedBeanWithProxyInThread() throws Exception {
		StepSynchronizationManager.register(new SimpleStepContext(null));
		final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		new Thread(new Runnable() {
			public void run() {
				TestBeanAware bean = (TestBeanAware) applicationContext.getBean("proxy");
				list.add(bean.getName());
			}
		}).start();
		int count = 0;
		while(list.size()==0 && count++ <10) {
			Thread.sleep(100);
		}
		if (list.size()==0) {
			fail("Scoped proxy was not created in child thread - maybe we need to use InheritableThreadLocal?");
		}
		String name = (String) list.get(0);
		assertEquals("spam", name);
	}

	public void testScopedBeanWithTwoProxiesInThreads() throws Exception {
		StepSynchronizationManager.register(new SimpleStepContext(null));
		final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		new Thread(new Runnable() {
			public void run() {
				TestBeanAware bean = (TestBeanAware) applicationContext.getBean("proxy");
				int count = 0;
				while(list.size()==0 && count++ <10) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						fail("Timeout waiting for other thread to add a bean to list.");
					}
				}
				bean.getName();
				list.add(bean);
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				TestBeanAware bean = (TestBeanAware) applicationContext.getBean("proxy");
				bean.getName();
				list.add(bean);
			}
		}).start();
		int count = 0;
		while(list.size()<2 && count++ <10) {
			Thread.sleep(100);
		}
		if (list.size()<2) {
			fail("Scoped proxies were not created in child threads");
		}
		TestBeanAware bean1 = (TestBeanAware) list.get(0);
		TestBeanAware bean2 = (TestBeanAware) list.get(1);
		assertEquals("spam", bean1.getName());
		assertSame(bean1.getLock(), bean2.getLock());
	}

	public static class TestBean {
		String name;
		TestBean child;
		public void setName(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setChild(TestBean child) {
			this.child = child;
		}
		public void close() {
			list.add("close");
		}
	}

	public static class TestBeanAware extends TestBean implements StepContextAware {
		AttributeAccessor context;
		Object lock = new Object();
		public void setStepContext(StepContext context) {
			this.context = context;
		}
		public AttributeAccessor getContext() {
			return context;
		}
		public Object getLock() {
			return lock;
		}
	}
}
