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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

/**
 * @author Dave Syer
 * 
 */
public class StepContextTests {

	private List<String> list = new ArrayList<String>();

	private StepExecution stepExecution = new StepExecution("step", new JobExecution(0L));

	private StepContext context = new StepContext(stepExecution);

	@Test
	public void testGetStepExecution() {
		context = new StepContext(stepExecution);
		assertNotNull(context.getStepExecution());
	}

	@Test
	public void testNullStepExecution() {
		try {
			context = new StepContext(null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testEqualsSelf() {
		assertEquals(context, context);
	}

	@Test
	public void testNotEqualsNull() {
		assertFalse(context.equals(null));
	}

	@Test
	public void testEqualsContextWithSameStepExecution() {
		assertEquals(new StepContext(stepExecution), context);
	}

	@Test
	public void testDestructionCallbackSunnyDay() throws Exception {
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

	@Test
	public void testDestructionCallbackMissingAttribute() throws Exception {
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

	@Test
	public void testDestructionCallbackWithException() throws Exception {
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
