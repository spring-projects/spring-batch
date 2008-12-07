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
package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @author Dave Syer
 * 
 */
public class StepScopeTests {

	private StepScope scope = new StepScope();

	private StepExecution stepExecution = new StepExecution("foo", new JobExecution(0L), 123L);

	private StepContext context;

	@Before
	public void setUp() throws Exception {
		context = StepSynchronizationManager.register(stepExecution);
	}

	@After
	public void tearDown() throws Exception {
		StepSynchronizationManager.close();
	}

	@Test
	public void testGetWithNoContext() throws Exception {
		final String foo = "bar";
		StepSynchronizationManager.close();
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

	@Test
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

	@Test
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

	@Test
	public void testGetWithSomethingAlreadyInParentContext() {
		context.setAttribute("foo", "bar");
		StepContext context = StepSynchronizationManager.register(new StepExecution("bar", new JobExecution(0L)));
		Object value = scope.get("foo", new ObjectFactory() {
			public Object getObject() throws BeansException {
				return "spam";
			}
		});
		assertEquals("spam", value);
		assertTrue(context.hasAttribute("foo"));
		StepSynchronizationManager.close();
		assertEquals("bar", scope.get("foo", null));
	}

	@Test
	public void testParentContextWithSameStepExecution() {
		context.setAttribute("foo", "bar");
		StepContext other = StepSynchronizationManager.register(stepExecution);
		assertSame(other, context);
	}

	@Test
	public void testGetConversationId() {
		String id = scope.getConversationId();
		assertNotNull(id);
	}

	@Test
	public void testRegisterDestructionCallback() {
		final List<String> list = new ArrayList<String>();
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

	@Test
	public void testRegisterAnotherDestructionCallback() {
		final List<String> list = new ArrayList<String>();
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

	@Test
	public void testRemove() {
		context.setAttribute("foo", "bar");
		scope.remove("foo");
		assertFalse(context.hasAttribute("foo"));
	}

	@Test
	public void testOrder() throws Exception {
		assertEquals(Integer.MAX_VALUE, scope.getOrder());
		scope.setOrder(11);
		assertEquals(11, scope.getOrder());
	}

	@Test
	public void testName() throws Exception {
		scope.setName("foo");
		StaticApplicationContext beanFactory = new StaticApplicationContext();
		scope.postProcessBeanFactory(beanFactory.getDefaultListableBeanFactory());
		String[] scopes = beanFactory.getDefaultListableBeanFactory().getRegisteredScopeNames();
		assertEquals(1, scopes.length);
		assertEquals("foo", scopes[0]);
	}

}
