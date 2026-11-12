/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.repository;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default methods of {@link JobRepository}.
 */
class JobRepositoryTests {

	@Test
	void testDefaultCreateStepExecutionWithContextFallsBackToTwoArgsVariant() {
		// given a repository that only implements createStepExecution(String,
		// JobExecution)
		JobRepository jobRepository = mock();
		JobExecution jobExecution = new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);
		ExecutionContext stepExecutionContext = new ExecutionContext();
		stepExecutionContext.putString("foo", "bar");
		when(jobRepository.createStepExecution("step", jobExecution)).thenReturn(stepExecution);
		doCallRealMethod().when(jobRepository).createStepExecution(any(), any(), any());

		// when
		StepExecution result = jobRepository.createStepExecution("step", jobExecution, stepExecutionContext);

		// then the context is set on the step execution and persisted afterwards
		assertSame(stepExecution, result);
		assertSame(stepExecutionContext, result.getExecutionContext());
		InOrder inOrder = inOrder(jobRepository);
		inOrder.verify(jobRepository).createStepExecution("step", jobExecution);
		inOrder.verify(jobRepository).updateExecutionContext(stepExecution);
	}

}
