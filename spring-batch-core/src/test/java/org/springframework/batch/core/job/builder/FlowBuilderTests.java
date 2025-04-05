/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.JobFlowExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.StepSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Injae Kim
 *
 */
class FlowBuilderTests {

	@Test
	void testNext() throws Exception {
		FlowBuilder<Flow> builder = new FlowBuilder<>("flow");
		JobRepository jobRepository = new JobRepositorySupport();
		JobExecution execution = jobRepository.createJobExecution("foo", new JobParameters());

		builder.next(createCompleteStep("stepA"))
			.end()
			.start(new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution));

		Iterator<StepExecution> stepExecutions = execution.getStepExecutions().iterator();
		assertEquals("stepA", stepExecutions.next().getStepName());
		assertFalse(stepExecutions.hasNext());
	}

	@Test
	void testMultipleNext() throws Exception {
		FlowBuilder<Flow> builder = new FlowBuilder<>("flow");
		JobRepository jobRepository = new JobRepositorySupport();
		JobExecution execution = jobRepository.createJobExecution("foo", new JobParameters());

		builder.next(createCompleteStep("stepA"))
			.next(createCompleteStep("stepB"))
			.next(createCompleteStep("stepC"))
			.end()
			.start(new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution));

		Iterator<StepExecution> stepExecutions = execution.getStepExecutions().iterator();
		assertEquals("stepA", stepExecutions.next().getStepName());
		assertEquals("stepB", stepExecutions.next().getStepName());
		assertEquals("stepC", stepExecutions.next().getStepName());
		assertFalse(stepExecutions.hasNext());
	}

	@Test
	void testStart() throws Exception {
		FlowBuilder<Flow> builder = new FlowBuilder<>("flow");
		JobRepository jobRepository = new JobRepositorySupport();
		JobExecution execution = jobRepository.createJobExecution("foo", new JobParameters());

		builder.start(createCompleteStep("stepA"))
			.end()
			.start(new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution));

		Iterator<StepExecution> stepExecutions = execution.getStepExecutions().iterator();
		assertEquals("stepA", stepExecutions.next().getStepName());
		assertFalse(stepExecutions.hasNext());
	}

	@Test
	void testFrom() throws Exception {
		FlowBuilder<Flow> builder = new FlowBuilder<>("flow");
		JobRepository jobRepository = new JobRepositorySupport();
		JobExecution execution = jobRepository.createJobExecution("foo", new JobParameters());

		builder.from(createCompleteStep("stepA"))
			.end()
			.start(new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution));

		Iterator<StepExecution> stepExecutions = execution.getStepExecutions().iterator();
		assertEquals("stepA", stepExecutions.next().getStepName());
		assertFalse(stepExecutions.hasNext());
	}

	@Test
	void testTransitionOrdering() throws Exception {
		FlowBuilder<Flow> builder = new FlowBuilder<>("transitionsFlow");
		JobRepository jobRepository = new JobRepositorySupport();
		JobExecution execution = jobRepository.createJobExecution("foo", new JobParameters());

		StepSupport stepA = new StepSupport("stepA") {
			@Override
			public void execute(StepExecution stepExecution) throws UnexpectedJobExecutionException {
				stepExecution.setExitStatus(ExitStatus.FAILED);
			}
		};

		StepSupport stepB = new StepSupport("stepB") {
			@Override
			public void execute(StepExecution stepExecution) throws UnexpectedJobExecutionException {
			}
		};

		StepSupport stepC = new StepSupport("stepC") {
			@Override
			public void execute(StepExecution stepExecution) throws UnexpectedJobExecutionException {
			}
		};

		FlowExecution flowExecution = builder.start(stepA)
			.on("*")
			.to(stepB)
			.from(stepA)
			.on("FAILED")
			.to(stepC)
			.end()
			.start(new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution));

		Iterator<StepExecution> stepExecutions = execution.getStepExecutions().iterator();
		assertEquals("stepA", stepExecutions.next().getStepName());
		assertEquals("stepC", stepExecutions.next().getStepName());
		assertFalse(stepExecutions.hasNext());
	}

	private static StepSupport createCompleteStep(String name) {
		return new StepSupport(name) {
			@Override
			public void execute(StepExecution stepExecution) throws UnexpectedJobExecutionException {
				stepExecution.upgradeStatus(BatchStatus.COMPLETED);
				stepExecution.setExitStatus(ExitStatus.COMPLETED);
			}
		};
	}

}
