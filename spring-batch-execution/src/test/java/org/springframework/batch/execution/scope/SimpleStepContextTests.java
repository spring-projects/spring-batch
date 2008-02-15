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

import org.springframework.batch.core.domain.StepExecution;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStepContextTests extends TestCase {

	private SimpleStepContext context = new SimpleStepContext(null, new SimpleStepContext(null));

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.SimpleStepContext#StepScopeContext()}.
	 */
	public void testStepScopeContext() {
		assertNull(new SimpleStepContext(null).getParent());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.SimpleStepContext#getParent()}.
	 */
	public void testGetParent() {
		assertNotNull(context.getParent());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.scope.SimpleStepContext#getStepExecution()}.
	 */
	public void testGetStepExecution() {
		assertNull(context.getStepExecution());
		context = new SimpleStepContext(new StepExecution(null, null, null));
		assertNotNull(context.getStepExecution());
	}

	private List list = new ArrayList();

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.context.SimpleStepContext#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	public void testDestructionCallbackSunnyDay() throws Exception {
		SimpleStepContext context = new SimpleStepContext(null);
		context.setAttribute("foo", "FOO");
		context.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("bar");
			}
		});
		context.close();
		assertEquals(1, list.size());
		assertEquals("bar", list.get(0));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.context.SimpleStepContext#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	public void testDestructionCallbackMissingAttribute() throws Exception {
		SimpleStepContext context = new SimpleStepContext(null);
		context.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("bar");
			}
		});
		context.close();
		// Yes the callback should be called even if the attribute is missing -
		// for inner beans
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.repeat.context.SimpleStepContext#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	public void testDestructionCallbackWithException() throws Exception {
		SimpleStepContext context = new SimpleStepContext(null);
		context.setAttribute("foo", "FOO");
		context.setAttribute("bar", "BAR");
		context.registerDestructionCallback("bar", new Runnable() {
			public void run() {
				list.add("spam");
				throw new RuntimeException("fail!");
			}
		});
		context.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("bar");
				throw new RuntimeException("fail!");
			}
		});
		try {
			context.close();
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// We don't care which one was thrown...
			assertEquals("fail!", e.getMessage());
		}
		// ...but we do care that both were executed:
		assertEquals(2, list.size());
		assertTrue(list.contains("bar"));
		assertTrue(list.contains("spam"));
	}

}
