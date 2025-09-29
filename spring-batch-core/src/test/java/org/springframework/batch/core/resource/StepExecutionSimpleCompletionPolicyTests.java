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

package org.springframework.batch.core.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.repeat.RepeatContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Unit tests for {@link StepExecutionSimpleCompletionPolicy}
 *
 * @author Dave Syer
 */
class StepExecutionSimpleCompletionPolicyTests {

	/**
	 * Object under test
	 */
	private final StepExecutionSimpleCompletionPolicy policy = new StepExecutionSimpleCompletionPolicy();

	/**
	 * mock step context
	 */
	@BeforeEach
	void setUp() {
		JobParameters jobParameters = new JobParametersBuilder().addLong("commit.interval", 2L).toJobParameters();
		JobInstance jobInstance = new JobInstance(0L, "testJob");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, jobParameters);
		Step step = new StepSupport("bar");
		StepExecution stepExecution = new StepExecution(1L, step.getName(), jobExecution);
		jobExecution.addStepExecution(stepExecution);
		policy.beforeStep(stepExecution);
	}

	@Test
	void testToString() {
		String msg = policy.toString();
		assertTrue("String does not contain chunk size", msg.contains("chunkSize=2"));
	}

	@Test
	void testKeyName() {
		RepeatContext context = policy.start(null);
		assertFalse(policy.isComplete(context));
	}

}
