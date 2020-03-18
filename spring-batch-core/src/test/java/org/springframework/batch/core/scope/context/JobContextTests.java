/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.scope.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 * @author Jimmy Praet
 */
public class JobContextTests {

	private List<String> list;

	private JobExecution jobExecution;

	private JobContext context;

	@Before
	public void setUp() {
		jobExecution = new JobExecution(1L);
		JobInstance jobInstance = new JobInstance(2L, "job");
		jobExecution.setJobInstance(jobInstance);
		context = new JobContext(jobExecution);
		list = new ArrayList<>();
	}

	@Test
	public void testGetJobExecution() {
		context = new JobContext(jobExecution);
		assertNotNull(context.getJobExecution());
	}

	@Test
	public void testNullJobExecution() {
		try {
			context = new JobContext(null);
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
	public void testEqualsContextWithSameJobExecution() {
		assertEquals(new JobContext(jobExecution), context);
	}

	@Test
	public void testDestructionCallbackSunnyDay() throws Exception {
		context.setAttribute("foo", "FOO");
		context.registerDestructionCallback("foo", new Runnable() {
			@Override
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
			@Override
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
			@Override
			public void run() {
				list.add("spam");
				throw new RuntimeException("fail!");
			}
		});
		context.registerDestructionCallback("foo", new Runnable() {
			@Override
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

	@Test
	public void testJobName() throws Exception {
		assertEquals("job", context.getJobName());
	}

	@Test
	public void testJobExecutionContext() throws Exception {
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		assertEquals("bar", context.getJobExecutionContext().get("foo"));
	}

	@Test
	public void testSystemProperties() throws Exception {
		System.setProperty("foo", "bar");
		assertEquals("bar", context.getSystemProperties().getProperty("foo"));
	}

	@Test
	public void testJobParameters() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobInstance jobInstance = new JobInstance(0L, "foo");
		jobExecution = new JobExecution(5L, jobParameters);
		jobExecution.setJobInstance(jobInstance);
		context = new JobContext(jobExecution);
		assertEquals("bar", context.getJobParameters().get("foo"));
	}

	@Test
	public void testContextId() throws Exception {
		assertEquals("jobExecution#1", context.getId());
	}

	@Test(expected = IllegalStateException.class)
	public void testIllegalContextId() throws Exception {
		context = new JobContext(new JobExecution((Long) null));
		context.getId();
	}

}
