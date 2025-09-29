/*
 * Copyright 2006-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 * @author Jimmy Praet
 * @author Mahmoud Ben Hassine
 */
class JobContextTests {

	private List<String> list;

	private JobExecution jobExecution;

	private JobContext context;

	@BeforeEach
	void setUp() {
		JobInstance jobInstance = new JobInstance(2L, "job");
		jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		context = new JobContext(jobExecution);
		list = new ArrayList<>();
	}

	@Test
	void testGetJobExecution() {
		context = new JobContext(jobExecution);
		assertNotNull(context.getJobExecution());
	}

	@Test
	void testNullJobExecution() {
		assertThrows(IllegalArgumentException.class, () -> new JobContext(null));
	}

	@Test
	void testEqualsSelf() {
		assertEquals(context, context);
	}

	@Test
	void testNotEqualsNull() {
		assertNotEquals(null, context);
	}

	@Test
	void testEqualsContextWithSameJobExecution() {
		assertEquals(new JobContext(jobExecution), context);
	}

	@Test
	void testDestructionCallbackSunnyDay() {
		context.setAttribute("foo", "FOO");
		context.registerDestructionCallback("foo", () -> list.add("bar"));
		context.close();
		assertEquals(1, list.size());
		assertEquals("bar", list.get(0));
	}

	@Test
	void testDestructionCallbackMissingAttribute() {
		context.registerDestructionCallback("foo", () -> list.add("bar"));
		context.close();
		// Yes the callback should be called even if the attribute is missing -
		// for inner beans
		assertEquals(1, list.size());
	}

	@Test
	void testDestructionCallbackWithException() {
		context.setAttribute("foo", "FOO");
		context.setAttribute("bar", "BAR");
		context.registerDestructionCallback("bar", () -> {
			list.add("spam");
			throw new RuntimeException("fail!");
		});
		context.registerDestructionCallback("foo", () -> {
			list.add("bar");
			throw new RuntimeException("fail!");
		});
		Exception exception = assertThrows(RuntimeException.class, () -> context.close());
		// We don't care which one was thrown...
		assertEquals("fail!", exception.getMessage());
		// ...but we do care that both were executed:
		assertEquals(2, list.size());
		assertTrue(list.contains("bar"));
		assertTrue(list.contains("spam"));
	}

	@Test
	void testJobName() {
		assertEquals("job", context.getJobName());
	}

	@Test
	void testJobExecutionContext() {
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		assertEquals("bar", context.getJobExecutionContext().get("foo"));
	}

	@Test
	void testSystemProperties() {
		System.setProperty("foo", "bar");
		assertEquals("bar", context.getSystemProperties().getProperty("foo"));
	}

	@Test
	void testJobParameters() {
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobInstance jobInstance = new JobInstance(0L, "foo");
		jobExecution = new JobExecution(5L, jobInstance, jobParameters);
		jobExecution.setJobInstance(jobInstance);
		context = new JobContext(jobExecution);
		assertEquals("bar", context.getJobParameters().get("foo"));
	}

	@Test
	void testContextId() {
		assertEquals("jobExecution#1", context.getId());
	}

}
