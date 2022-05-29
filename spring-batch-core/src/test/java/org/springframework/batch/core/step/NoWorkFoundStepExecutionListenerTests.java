/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.batch.core.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

/**
 * Tests for {@link NoWorkFoundStepExecutionListener}.
 */
public class NoWorkFoundStepExecutionListenerTests {

	private NoWorkFoundStepExecutionListener tested = new NoWorkFoundStepExecutionListener();

	@Test
	public void noWork() {
		StepExecution stepExecution = new StepExecution("NoProcessingStep",
				new JobExecution(new JobInstance(1L, "NoProcessingJob"), new JobParameters()));

		stepExecution.setExitStatus(ExitStatus.COMPLETED);
		stepExecution.setReadCount(0);

		ExitStatus exitStatus = tested.afterStep(stepExecution);
		assertEquals(ExitStatus.FAILED.getExitCode(), exitStatus.getExitCode());
	}

	@Test
	public void workDone() {
		StepExecution stepExecution = new StepExecution("NoProcessingStep",
				new JobExecution(new JobInstance(1L, "NoProcessingJob"), new JobParameters()));

		stepExecution.setReadCount(1);

		ExitStatus exitStatus = tested.afterStep(stepExecution);
		assertNull(exitStatus);
	}

}
