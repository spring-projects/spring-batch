/*
 * Copyright 2024 the original author or authors.
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

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractStep}.
 */
class AbstractStepTests {

	@Test
	void testEndTimeInListener() throws Exception {
		// given
		StepExecution execution = new StepExecution("step",
				new JobExecution(new JobInstance(1L, "job"), new JobParameters()));
		AbstractStep tested = new AbstractStep() {
			@Override
			protected void doExecute(StepExecution stepExecution) {
			}
		};
		JobRepository jobRepository = mock();
		Listener stepListener = new Listener();
		tested.setStepExecutionListeners(new StepExecutionListener[] { stepListener });
		tested.setJobRepository(jobRepository);

		// when
		tested.execute(execution);

		// then
		assertNotNull(stepListener.getStepEndTime());
	}

	static class Listener implements StepExecutionListener {

		private LocalDateTime stepEndTime;

		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			this.stepEndTime = stepExecution.getEndTime();
			return ExitStatus.COMPLETED;
		}

		public LocalDateTime getStepEndTime() {
			return this.stepEndTime;
		}

	}

}
