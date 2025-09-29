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
package org.springframework.batch.core.step.tasklet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;

/**
 * @author Dave Syer
 *
 */
class StepHandlerAdapterTests {

	private final MethodInvokingTaskletAdapter tasklet = new MethodInvokingTaskletAdapter();

	private Object result = null;

	private final StepExecution stepExecution = new StepExecution("systemCommandStep",
			new JobExecution(1L, new JobInstance(1L, "systemCommandJob"), new JobParameters()));

	public ExitStatus execute() {
		return ExitStatus.NOOP;
	}

	public Object process() {
		return result;
	}

	@BeforeEach
	void setUp() {
		tasklet.setTargetObject(this);
	}

	@Test
	void testExecuteWithExitStatus() throws Exception {
		tasklet.setTargetMethod("execute");
		StepContribution contribution = stepExecution.createStepContribution();
		tasklet.execute(contribution, null);
		assertEquals(ExitStatus.NOOP, contribution.getExitStatus());
	}

	@Test
	void testMapResultWithNull() throws Exception {
		tasklet.setTargetMethod("process");
		StepContribution contribution = stepExecution.createStepContribution();
		tasklet.execute(contribution, null);
		assertEquals(ExitStatus.COMPLETED, contribution.getExitStatus());
	}

	@Test
	void testMapResultWithNonNull() throws Exception {
		tasklet.setTargetMethod("process");
		this.result = "foo";
		StepContribution contribution = stepExecution.createStepContribution();
		tasklet.execute(contribution, null);
		assertEquals(ExitStatus.COMPLETED, contribution.getExitStatus());
	}

}
