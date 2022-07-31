/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

class ExecutionContextTestUtilsTests {

	@Test
	void testFromJob() {
		Date date = new Date();
		JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution();
		jobExecution.getExecutionContext().put("foo", date);
		Date result = ExecutionContextTestUtils.getValueFromJob(jobExecution, "foo");
		assertEquals(date, result);
	}

	@Test
	void testFromStepInJob() {
		Date date = new Date();
		JobExecution jobExecution = MetaDataInstanceFactory.createJobExecutionWithStepExecutions(123L,
				Arrays.asList("foo", "bar"));
		StepExecution stepExecution = jobExecution.createStepExecution("spam");
		stepExecution.getExecutionContext().put("foo", date);
		Date result = ExecutionContextTestUtils.getValueFromStepInJob(jobExecution, "spam", "foo");
		assertEquals(date, result);
	}

	@Test
	void testFromStepInJobNoSuchStep() {
		JobExecution jobExecution = MetaDataInstanceFactory.createJobExecutionWithStepExecutions(123L,
				Arrays.asList("foo", "bar"));
		assertThrows(IllegalArgumentException.class,
				() -> ExecutionContextTestUtils.getValueFromStepInJob(jobExecution, "spam", "foo"));
	}

	@Test
	void testFromStep() {
		Date date = new Date();
		StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();
		stepExecution.getExecutionContext().put("foo", date);
		Date result = ExecutionContextTestUtils.getValueFromStep(stepExecution, "foo");
		assertEquals(date, result);
	}

}
