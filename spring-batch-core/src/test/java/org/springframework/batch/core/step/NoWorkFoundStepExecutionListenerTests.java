/*
 * Copyright 2002-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;

/**
 * Tests for {@link NoWorkFoundStepExecutionListener}.
 */
class NoWorkFoundStepExecutionListenerTests {

	private final NoWorkFoundStepExecutionListener tested = new NoWorkFoundStepExecutionListener();

	@Test
	void noWork() {
		StepExecution stepExecution = new StepExecution("NoProcessingStep",
				new JobExecution(new JobInstance(1L, "NoProcessingJob"), new JobParameters()));

		stepExecution.setExitStatus(ExitStatus.COMPLETED);
		stepExecution.setReadCount(0);

		ExitStatus exitStatus = tested.afterStep(stepExecution);
		assertEquals(ExitStatus.FAILED.getExitCode(), exitStatus.getExitCode());
	}

	@Test
	void workDone() {
		StepExecution stepExecution = new StepExecution("NoProcessingStep",
				new JobExecution(new JobInstance(1L, "NoProcessingJob"), new JobParameters()));

		stepExecution.setReadCount(1);

		ExitStatus exitStatus = tested.afterStep(stepExecution);
		assertNull(exitStatus);
	}

}
