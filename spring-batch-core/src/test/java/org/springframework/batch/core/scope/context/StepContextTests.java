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

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 * @author Nicolas Widart
 * @author Mahmoud Ben Hassine
 *
 */
class StepContextTests {

	private final List<String> list = new ArrayList<>();

	private StepExecution stepExecution = new StepExecution("step",
			new JobExecution(new JobInstance(2L, "job"), 0L, null), 1L);

	private StepContext context = new StepContext(stepExecution);

	@Test
	void testGetStepExecution() {
		context = new StepContext(stepExecution);
		assertNotNull(context.getStepExecution());
	}

	@Test
	void testNullStepExecution() {
		assertThrows(IllegalArgumentException.class, () -> new StepContext(null));
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
	void testEqualsContextWithSameStepExecution() {
		assertEquals(new StepContext(stepExecution), context);
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
	void testStepName() {
		assertEquals("step", context.getStepName());
	}

	@Test
	void testJobName() {
		assertEquals("job", context.getJobName());
	}

	@Test
	void testJobInstanceId() {
		assertEquals(2L, (long) context.getJobInstanceId());
	}

	@Test
	void testStepExecutionContext() {
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		assertEquals("bar", context.getStepExecutionContext().get("foo"));
	}

	@Test
	void testSystemProperties() {
		System.setProperty("foo", "bar");
		assertEquals("bar", context.getSystemProperties().getProperty("foo"));
	}

	@Test
	void testJobExecutionContext() {
		ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
		executionContext.put("foo", "bar");
		assertEquals("bar", context.getJobExecutionContext().get("foo"));
	}

	@Test
	void testJobParameters() {
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobInstance instance = stepExecution.getJobExecution().getJobInstance();
		stepExecution = new StepExecution("step", new JobExecution(instance, jobParameters));
		context = new StepContext(stepExecution);
		assertEquals("bar", context.getJobParameters().get("foo"));
	}

	@Test
	void testContextId() {
		assertEquals("execution#1", context.getId());
	}

	@Test
	void testIllegalContextId() {
		context = new StepContext(new StepExecution("foo", new JobExecution(0L)));
		assertThrows(IllegalStateException.class, context::getId);
	}

}
