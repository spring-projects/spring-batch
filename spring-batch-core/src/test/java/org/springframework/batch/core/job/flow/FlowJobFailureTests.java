/*
 * Copyright 2010-2026 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * Test suite for various failure scenarios during job processing.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class FlowJobFailureTests {

	private final FlowJob job = new FlowJob();

	private JobExecution execution;

	private JobRepository jobRepository;

	@BeforeEach
	void init() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.generateUniqueName(true)
			.build();
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		job.setJobRepository(jobRepository);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("job", jobParameters);
		execution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

	}

	@Test
	void testStepFailure() throws Exception {
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
	void testUnmatchedTransitionDoesNotAffectOtherJob() throws Exception {
		job.setFlow(failingFlow());
		job.afterPropertiesSet();
		job.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		StepExecution failedStep = jobRepository.getLastStepExecution(execution.getJobInstance(), "step");
		assertEquals(BatchStatus.FAILED, failedStep.getStatus());

		FlowJob otherJob = new FlowJob("otherJob");
		otherJob.setJobRepository(jobRepository);
		SimpleFlow otherFlow = new SimpleFlow("otherFlow");
		StepState otherStep = new StepState(new StepSupport("otherStep") {
			@Override
			public void execute(StepExecution stepExecution) {
				stepExecution.setStatus(BatchStatus.COMPLETED);
				stepExecution.setExitStatus(ExitStatus.COMPLETED);
				jobRepository.update(stepExecution);
			}
		});
		otherFlow.setStateTransitions(List.of(StateTransition.createEndStateTransition(otherStep)));
		otherJob.setFlow(otherFlow);
		otherJob.afterPropertiesSet();
		JobParameters parameters = new JobParameters();
		JobInstance instance = jobRepository.createJobInstance("otherJob", parameters);
		JobExecution otherExecution = jobRepository.createJobExecution(instance, parameters, new ExecutionContext());
		otherJob.execute(otherExecution);
		assertEquals(BatchStatus.COMPLETED, otherExecution.getStatus());

		StepExecution reloaded = jobRepository.getLastStepExecution(execution.getJobInstance(), "step");
		assertEquals(BatchStatus.FAILED, reloaded.getStatus());
		assertEquals(failedStep.getVersion(), reloaded.getVersion());
	}

	@Test
	void testUnmatchedTransitionDoesNotPreventRestart() throws Exception {
		job.setFlow(failingFlow());
		job.afterPropertiesSet();
		job.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());

		JobExecution restart = jobRepository.createJobExecution(execution.getJobInstance(), new JobParameters(),
				new ExecutionContext());
		job.execute(restart);

		assertEquals(BatchStatus.FAILED, restart.getStatus());
		assertEquals(1, restart.getStepExecutions().size());
		assertEquals(2, jobRepository.getStepExecutionCount(execution.getJobInstance(), "step"));
		assertEquals(BatchStatus.FAILED,
				jobRepository.getLastStepExecution(execution.getJobInstance(), "step").getStatus());
	}

	@Test
	void testUnmatchedTransitionClosesExecutor() throws Exception {
		JobFlowExecutor executor = new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution);
		SimpleFlow flow = failingFlow();
		try {
			FlowExecutionException exception = assertThrows(FlowExecutionException.class, () -> flow.start(executor));
			assertTrue(exception.getMessage().contains("Next state not found"));
			assertNull(executor.getStepExecution());
		}
		finally {
			executor.close(new FlowExecution("step", FlowExecutionStatus.FAILED));
		}
	}

	@Test
	void testExecutorsKeepSeparateStepExecutions() throws Exception {
		JobExecution otherExecution = jobRepository.createJobExecution(execution.getJobInstance(), new JobParameters(),
				new ExecutionContext());
		JobFlowExecutor executor = new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository), execution);
		JobFlowExecutor otherExecutor = new JobFlowExecutor(jobRepository, new SimpleStepHandler(jobRepository),
				otherExecution);
		try {
			executor.executeStep(failingStep());
			StepExecution stepExecution = executor.getStepExecution();
			assertNull(otherExecutor.getStepExecution());
			otherExecutor.close(new FlowExecution("other", FlowExecutionStatus.COMPLETED));
			assertSame(stepExecution, executor.getStepExecution());
		}
		finally {
			executor.close(new FlowExecution("step", FlowExecutionStatus.FAILED));
		}
	}

	private SimpleFlow failingFlow() {
		SimpleFlow flow = new SimpleFlow("job");
		flow.setStateTransitions(
				List.of(StateTransition.createEndStateTransition(new StepState(failingStep()), "COMPLETED")));
		return flow;
	}

	private Step failingStep() {
		return new StepSupport("step") {
			@Override
			public void execute(StepExecution stepExecution) {
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExitStatus(ExitStatus.FAILED);
				jobRepository.update(stepExecution);
			}
		};
	}

	@Test
	void testStepStatusUnknown() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		StepState step = new StepState(new StepSupport("step") {
			@Override
			public void execute(StepExecution stepExecution)
					throws JobInterruptedException, UnexpectedJobExecutionException {
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
