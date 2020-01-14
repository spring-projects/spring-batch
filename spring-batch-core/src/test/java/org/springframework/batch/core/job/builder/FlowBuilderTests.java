/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.Test;

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
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 * @author Michael Minella
 * 
 */
public class FlowBuilderTests {

	@Test
	public void test() throws Exception {
		FlowBuilder<Flow> builder = new FlowBuilder<>("flow");
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		JobExecution execution = jobRepository.createJobExecution("foo", new JobParameters());
		builder.start(new StepSupport("step") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
			}
		}).end().start(new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution));
	}

	@Test
	public void testTransitionOrdering() throws Exception {
		FlowBuilder<Flow> builder = new FlowBuilder<>("transitionsFlow");
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		JobExecution execution = jobRepository.createJobExecution("foo", new JobParameters());

		StepSupport stepA = new StepSupport("stepA") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
				stepExecution.setExitStatus(new ExitStatus("FAILED"));
			}
		};

		StepSupport stepB = new StepSupport("stepB") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
			}
		};

		StepSupport stepC = new StepSupport("stepC") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
			}
		};

		FlowExecution flowExecution = builder.start(stepA)
				.on("*").to(stepB)
				.from(stepA).on("FAILED").to(stepC)
				.end().start(new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution));

		Iterator<StepExecution> stepExecutions = execution.getStepExecutions().iterator();
		StepExecution stepExecutionA = stepExecutions.next();
		assertEquals(stepExecutionA.getStepName(), "stepA");
		StepExecution stepExecutionC = stepExecutions.next();
		assertEquals(stepExecutionC.getStepName(), "stepC");
	}
}
