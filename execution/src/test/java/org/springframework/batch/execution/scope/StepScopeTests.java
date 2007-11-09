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

import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @author Dave Syer
 * 
 */
public class StepScopeTests extends TestCase {

	private StepScope scope = new StepScope();

	private SimpleStepContext context;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		context = StepSynchronizationManager.open();
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		RepeatSynchronizationManager.clear();		
		super.tearDown();
	}

	public void testGetWithNoContext() throws Exception {
		final String foo = "bar";
		StepSynchronizationManager.clear();
		try {
			scope.get("foo", new ObjectFactory() {
				public Object getObject() throws BeansException {
					return foo;
				}
			});
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}

	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#get(java.lang.String, org.springframework.beans.factory.ObjectFactory)}.
	 */
	public void testGetWithNothingAlreadyThere() {
		final String foo = "bar";
		Object value = scope.get("foo", new ObjectFactory() {
			public Object getObject() throws BeansException {
				return foo;
			}
		});
		assertEquals(foo, value);
		assertTrue(context.hasAttribute("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#get(java.lang.String, org.springframework.beans.factory.ObjectFactory)}.
	 */
	public void testGetWithSomethingAlreadyThere() {
		context.setAttribute("foo", "bar");
		Object value = scope.get("foo", new ObjectFactory() {
			public Object getObject() throws BeansException {
				return null;
			}
		});
		assertEquals("bar", value);
		assertTrue(context.hasAttribute("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#get(java.lang.String, org.springframework.beans.factory.ObjectFactory)}.
	 */
	public void testGetWithSomethingAlreadyInParentContext() {
		SimpleStepContext context = StepSynchronizationManager.open();
		context.setAttribute("foo", "bar");
		Object value = scope.get("foo", new ObjectFactory() {
			public Object getObject() throws BeansException {
				return null;
			}
		});
		assertEquals("bar", value);
		assertTrue(context.hasAttribute("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#getConversationId()}.
	 */
	public void testGetConversationId() {
		String id = scope.getConversationId();
		assertNotNull(id);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#getConversationId()}.
	 */
	public void testGetConversationIdFromAttribute() {
		context.setAttribute(StepScope.ID_KEY, "foo");
		String id = scope.getConversationId();
		assertEquals("foo", id);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	public void testRegisterDestructionCallback() {
		final List list = new ArrayList();
		context.setAttribute("foo", "bar");
		scope.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("foo");
			}
		});
		assertEquals(0, list.size());
		// When the context is closed, provided the attribute exists the
		// callback is called...
		context.close();
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	public void testRegisterAnotherDestructionCallback() {
		final List list = new ArrayList();
		context.setAttribute("foo", "bar");
		scope.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("foo");
			}
		});
		scope.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("bar");
			}
		});
		assertEquals(0, list.size());
		// When the context is closed, provided the attribute exists the
		// callback is called...
		context.close();
		assertEquals(2, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.StepScope#remove(java.lang.String)}.
	 */
	public void testRemove() {
		context.setAttribute("foo", "bar");
		scope.remove("foo");
		assertFalse(context.hasAttribute("foo"));
	}
	
	public void testOrder() throws Exception {
		assertEquals(Integer.MAX_VALUE, scope.getOrder());
		scope.setOrder(11);
		assertEquals(11, scope.getOrder());
	}
	
	public void testName() throws Exception {
		scope.setName("foo");
		StaticApplicationContext beanFactory = new StaticApplicationContext();
		scope.postProcessBeanFactory(beanFactory.getDefaultListableBeanFactory());
		String[] scopes = beanFactory.getDefaultListableBeanFactory().getRegisteredScopeNames();
		assertEquals(1, scopes.length);
		assertEquals("foo", scopes[0]);
	}

}
