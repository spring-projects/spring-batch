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
package org.springframework.batch.core.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;

/**
 * @author Dave Syer
 *
 */
class JobParameterExecutionContextCopyListenerTests {

	private final JobParameterExecutionContextCopyListener listener = new JobParameterExecutionContextCopyListener();

	private StepExecution stepExecution;

	@BeforeEach
	void createExecution() {
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		stepExecution = new StepExecution("foo", new JobExecution(1L, new JobInstance(123L, "job"), jobParameters));
	}

	@Test
	void testBeforeStep() {
		listener.beforeStep(stepExecution);
		assertEquals("bar", stepExecution.getExecutionContext().get("foo"));
	}

	@Test
	void testSetKeys() {
		listener.setKeys(new String[] {});
		listener.beforeStep(stepExecution);
		assertFalse(stepExecution.getExecutionContext().containsKey("foo"));
	}

}
