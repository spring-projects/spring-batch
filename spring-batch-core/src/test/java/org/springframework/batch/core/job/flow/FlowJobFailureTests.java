/*
 * Copyright 2010-2014 the original author or authors.
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
package org.springframework.batch.core.job.flow;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;

/**
 * Test suite for various failure scenarios during job processing.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class FlowJobFailureTests {

	private FlowJob job = new FlowJob();

	private JobExecution execution;

	@Before
	public void init() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		job.setJobRepository(jobRepository);
		execution = jobRepository.createJobExecution("job", new JobParameters());
	}

	@Test
	public void testStepFailure() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		StepState step = new StepState(new StepSupport("step"));
		transitions.add(StateTransition.createStateTransition(step, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step, ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	@Test
	public void testStepStatusUnknown() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		StepState step = new StepState(new StepSupport("step") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
				// This is what happens if the repository meta-data cannot be
				// updated
				stepExecution.setExitStatus(ExitStatus.UNKNOWN);
				stepExecution.setStatus(BatchStatus.UNKNOWN);
			}
		});
		transitions.add(StateTransition.createStateTransition(step, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step, "*", "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.execute(execution);
		assertEquals(BatchStatus.UNKNOWN, execution.getStatus());
	}

}
