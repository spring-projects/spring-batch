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
		StepSynchronizationManager.open();
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		TestBean bean = (TestBean) applicationContext.getBean("bean");
		assertNotNull(bean);
		assertEquals("foo", bean.name);
	}
	
	public void testScopedBeanWithDestroyCallback() throws Exception {
		assertEquals(0, list.size());
		StepSynchronizationManager.open();
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		TestBean bean = (TestBean) applicationContext.getBean("bean");
		assertNotNull(bean);
		StepSynchronizationManager.close();
		assertEquals(1, list.size());
	}

	public void testScopedBeanWithAware() throws Exception {
		StepContext context = StepSynchronizationManager.open();
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("scope-tests.xml", getClass());
		TestBeanAware bean = (TestBeanAware) applicationContext.getBean("aware");
		assertNotNull(bean);
		assertEquals("bar", bean.name);
		assertEquals(context, bean.context);
	}

	public void testScopedBeanWithInner() throws Exception {
		StepSynchronizationManager.open();
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"scope-tests.xml", getClass());
		TestBean bean = ((TestBean) applicationContext.getBean("inner")).child;
		assertNotNull(bean);
		assertEquals("bar", bean.name);
		StepSynchronizationManager.close();
		// TODO: Still a bug in Spring Core?  Preventing destroy method from being called in inner bean.
		// assertEquals(1, list.size());
	}

	public static class TestBean {
		String name;
		TestBean child;
		public void setName(String name) {
			this.name = name;
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
		public void setStepContext(StepContext context) {
			this.context = context;
		}
	}
}
