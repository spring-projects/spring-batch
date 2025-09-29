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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class ChunkContextTests {

	private ChunkContext context;

	@BeforeEach
	void setUp() {
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance,
				new JobParameters(Collections.singletonMap("foo", new JobParameter<>("bar", String.class))));
		StepExecution stepExecution = new StepExecution(1L, "foo", jobExecution);
		context = new ChunkContext(new StepContext(stepExecution));
	}

	@Test
	void testGetStepContext() {
		StepContext stepContext = context.getStepContext();
		assertNotNull(stepContext);
		assertEquals("bar", context.getStepContext().getJobParameters().get("foo"));
	}

	@Test
	void testIsComplete() {
		assertFalse(context.isComplete());
		context.setComplete();
		assertTrue(context.isComplete());
	}

	@Test
	void testToString() {
		String value = context.toString();
		assertTrue(value.contains("stepContext="), "Wrong toString: " + value);
		assertTrue(value.contains("complete=false"), "Wrong toString: " + value);
		assertTrue(value.contains("attributes=[]"), "Wrong toString: " + value);
	}

}
