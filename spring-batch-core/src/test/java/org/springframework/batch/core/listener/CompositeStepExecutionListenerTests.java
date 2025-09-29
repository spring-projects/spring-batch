/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.lang.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class CompositeStepExecutionListenerTests {

	private final CompositeStepExecutionListener listener = new CompositeStepExecutionListener();

	private final List<String> list = new ArrayList<>();

	@Test
	void testSetListeners() {
		JobExecution jobExecution = new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters());
		StepExecution stepExecution = new StepExecution("s1", jobExecution);
		listener.setListeners(new StepExecutionListener[] { new StepExecutionListener() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				assertEquals(ExitStatus.STOPPED, stepExecution.getExitStatus());
				list.add("fail");
				return ExitStatus.FAILED;
			}
		}, new StepExecutionListener() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("continue");
				return ExitStatus.STOPPED;
			}
		} });
		assertEquals(ExitStatus.FAILED, listener.afterStep(stepExecution));
		assertEquals(2, list.size());
	}

	@Test
	void testSetListener() {
		JobExecution jobExecution = new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters());
		StepExecution stepExecution = new StepExecution("s1", jobExecution);
		listener.register(new StepExecutionListener() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("fail");
				return ExitStatus.FAILED;
			}
		});
		assertEquals(ExitStatus.FAILED, listener.afterStep(stepExecution));
		assertEquals(1, list.size());
	}

	@Test
	void testOpen() {
		listener.register(new StepExecutionListener() {
			@Override
			public void beforeStep(StepExecution stepExecution) {
				list.add("foo");
			}
		});
		listener.beforeStep(new StepExecution("foo", null));
		assertEquals(1, list.size());
	}

}
