/*
 * Copyright 2006-2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.repository.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.job.flow.support.DefaultStateTransitionComparator;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.DecisionState;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.FlowState;
import org.springframework.batch.core.job.flow.support.state.SplitState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
public class FlowJobTests {

	private final FlowJob job = new FlowJob();

	private JobExecution jobExecution;

	private JobRepository jobRepository;

	private JobExplorer jobExplorer;

	private boolean fail = false;

	@SuppressWarnings("removal")
	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(transactionManager);
		factory.afterPropertiesSet();
		this.jobRepository = factory.getObject();
		job.setJobRepository(this.jobRepository);
		this.jobExecution = this.jobRepository.createJobExecution("job", new JobParameters());

		JobExplorerFactoryBean explorerFactoryBean = new JobExplorerFactoryBean();
		explorerFactoryBean.setDataSource(embeddedDatabase);
		explorerFactoryBean.setTransactionManager(transactionManager);
		explorerFactoryBean.afterPropertiesSet();
		this.jobExplorer = explorerFactoryBean.getObject();
	}

	@Test
	void testGetSteps() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();
		assertEquals(2, job.getStepNames().size());
	}

	@Test
	void testTwoSteps() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.doExecute(jobExecution);
		StepExecution stepExecution = getStepExecution(jobExecution, "step2");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());
	}

	@Test
	void testFailedStep() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions
			.add(StateTransition.createStateTransition(new StateSupport("step1", FlowExecutionStatus.FAILED), "step2"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.doExecute(jobExecution);
		StepExecution stepExecution = getStepExecution(jobExecution, "step2");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());
	}

	@Test
	void testFailedStepRestarted() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		State step2State = new StateSupport("step2") {
			@Override
			public FlowExecutionStatus handle(FlowExecutor executor) throws Exception {
				JobExecution jobExecution = executor.getJobExecution();
				jobExecution.createStepExecution(getName());
				if (fail) {
					return FlowExecutionStatus.FAILED;
				}
				else {
					return FlowExecutionStatus.COMPLETED;
				}
			}
		};
		transitions.add(StateTransition.createStateTransition(step2State, ExitStatus.COMPLETED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2State, ExitStatus.FAILED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		fail = true;
		job.execute(jobExecution);
		assertEquals(ExitStatus.FAILED, jobExecution.getExitStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());
		jobRepository.update(jobExecution);
		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		fail = false;
		job.execute(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	@Test
	void testStoppingStep() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		State state2 = new StateSupport("step2", FlowExecutionStatus.FAILED);
		transitions.add(StateTransition.createStateTransition(state2, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(state2, ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions
			.add(StateTransition.createStateTransition(new EndState(FlowExecutionStatus.STOPPED, "end0"), "step3"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step3")), "end2"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end2")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.doExecute(jobExecution);
		assertEquals(2, jobExecution.getStepExecutions().size());
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
	}

	@Test
	void testInterrupted() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				stepExecution.setStatus(BatchStatus.STOPPING);
				jobRepository.update(stepExecution);
			}
		}), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		checkRepository(BatchStatus.STOPPED, ExitStatus.STOPPED);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(JobInterruptedException.class, jobExecution.getFailureExceptions().get(0).getClass());
	}

	@Test
	void testUnknownStatusStopsJob() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				stepExecution.setTerminateOnly();
				jobRepository.update(stepExecution);
			}
		}), "step2"));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.execute(jobExecution);
		assertEquals(BatchStatus.UNKNOWN, jobExecution.getStatus());
		checkRepository(BatchStatus.UNKNOWN, ExitStatus.STOPPED);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(JobInterruptedException.class, jobExecution.getFailureExceptions().get(0).getClass());
	}

	@Test
	void testInterruptedSplit() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		SimpleFlow flow1 = new SimpleFlow("flow1");
		SimpleFlow flow2 = new SimpleFlow("flow2");

		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				if (!stepExecution.getJobExecution().getExecutionContext().containsKey("STOPPED")) {
					stepExecution.getJobExecution().getExecutionContext().put("STOPPED", true);
					stepExecution.setStatus(BatchStatus.STOPPED);
					jobRepository.update(stepExecution);
				}
				else {
					fail("The Job should have stopped by now");
				}
			}
		}), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow1.setStateTransitions(new ArrayList<>(transitions));
		flow1.afterPropertiesSet();
		flow2.setStateTransitions(new ArrayList<>(transitions));
		flow2.afterPropertiesSet();

		transitions = new ArrayList<>();
		transitions.add(StateTransition
			.createStateTransition(new SplitState(Arrays.<Flow>asList(flow1, flow2), "split"), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();

		job.setFlow(flow);
		job.afterPropertiesSet();
		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		checkRepository(BatchStatus.STOPPED, ExitStatus.STOPPED);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(JobInterruptedException.class, jobExecution.getFailureExceptions().get(0).getClass());
		assertEquals(1, jobExecution.getStepExecutions().size());
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
		}
	}

	@Test
	void testInterruptedException() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				throw new JobInterruptedException("Stopped");
			}
		}), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		checkRepository(BatchStatus.STOPPED, ExitStatus.STOPPED);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(JobInterruptedException.class, jobExecution.getFailureExceptions().get(0).getClass());
	}

	@Test
	void testInterruptedSplitException() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		SimpleFlow flow1 = new SimpleFlow("flow1");
		SimpleFlow flow2 = new SimpleFlow("flow2");

		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				throw new JobInterruptedException("Stopped");
			}
		}), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow1.setStateTransitions(new ArrayList<>(transitions));
		flow1.afterPropertiesSet();
		flow2.setStateTransitions(new ArrayList<>(transitions));
		flow2.afterPropertiesSet();

		transitions = new ArrayList<>();
		transitions.add(StateTransition
			.createStateTransition(new SplitState(Arrays.<Flow>asList(flow1, flow2), "split"), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();

		job.setFlow(flow);
		job.afterPropertiesSet();
		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		checkRepository(BatchStatus.STOPPED, ExitStatus.STOPPED);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(JobInterruptedException.class, jobExecution.getFailureExceptions().get(0).getClass());
	}

	@Test
	void testEndStateStopped() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "end"));
		transitions
			.add(StateTransition.createStateTransition(new EndState(FlowExecutionStatus.STOPPED, "end"), "step2"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.doExecute(jobExecution);
		assertEquals(1, jobExecution.getStepExecutions().size());
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
	}

	public void testEndStateFailed() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "end"));
		transitions
			.add(StateTransition.createStateTransition(new EndState(FlowExecutionStatus.FAILED, "end"), "step2"));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")),
				ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")),
				ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.doExecute(jobExecution);
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	@Test
	void testEndStateStoppedWithRestart() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "end"));
		transitions
			.add(StateTransition.createStateTransition(new EndState(FlowExecutionStatus.STOPPED, "end"), "step2"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end1")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.afterPropertiesSet();

		// To test a restart we have to use the AbstractJob.execute()...
		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());

		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		job.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());

	}

	@Test
	void testBranching() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		StepState step1 = new StepState(new StubStep("step1"));
		transitions.add(StateTransition.createStateTransition(step1, "step2"));
		transitions.add(StateTransition.createStateTransition(step1, "COMPLETED", "step3"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end1")));
		StepState step3 = new StepState(new StubStep("step3"));
		transitions.add(StateTransition.createStateTransition(step3, ExitStatus.FAILED.getExitCode(), "end2"));
		transitions.add(StateTransition.createStateTransition(step3, ExitStatus.COMPLETED.getExitCode(), "end3"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end2")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end3")));
		flow.setStateTransitions(transitions);
		flow.setStateTransitionComparator(new DefaultStateTransitionComparator());
		job.setFlow(flow);
		job.afterPropertiesSet();
		job.doExecute(jobExecution);
		StepExecution stepExecution = getStepExecution(jobExecution, "step3");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());
	}

	@Test
	void testBasicFlow() throws Throwable {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		job.setFlow(flow);
		job.execute(jobExecution);
		if (!jobExecution.getAllFailureExceptions().isEmpty()) {
			throw jobExecution.getAllFailureExceptions().get(0);
		}
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	void testDecisionFlow() throws Throwable {

		SimpleFlow flow = new SimpleFlow("job");
		JobExecutionDecider decider = (jobExecution, stepExecution) -> {
			assertNotNull(stepExecution);
			return new FlowExecutionStatus("SWITCH");
		};

		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "decision"));
		DecisionState decision = new DecisionState(decider, "decision");
		transitions.add(StateTransition.createStateTransition(decision, "step2"));
		transitions.add(StateTransition.createStateTransition(decision, "SWITCH", "step3"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end1")));
		StepState step3 = new StepState(new StubStep("step3"));
		transitions.add(StateTransition.createStateTransition(step3, ExitStatus.FAILED.getExitCode(), "end2"));
		transitions.add(StateTransition.createStateTransition(step3, ExitStatus.COMPLETED.getExitCode(), "end3"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end2")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end3")));
		flow.setStateTransitions(transitions);
		flow.setStateTransitionComparator(new DefaultStateTransitionComparator());

		job.setFlow(flow);
		job.doExecute(jobExecution);
		StepExecution stepExecution = getStepExecution(jobExecution, "step3");
		if (!jobExecution.getAllFailureExceptions().isEmpty()) {
			throw jobExecution.getAllFailureExceptions().get(0);
		}

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());

	}

	@Test
	void testDecisionFlowWithExceptionInDecider() throws Throwable {

		SimpleFlow flow = new SimpleFlow("job");
		JobExecutionDecider decider = (jobExecution, stepExecution) -> {
			assertNotNull(stepExecution);
			throw new RuntimeException("Foo");
		};

		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "decision"));
		DecisionState decision = new DecisionState(decider, "decision");
		transitions.add(StateTransition.createStateTransition(decision, "step2"));
		transitions.add(StateTransition.createStateTransition(decision, "SWITCH", "step3"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end1")));
		StepState step3 = new StepState(new StubStep("step3"));
		transitions.add(StateTransition.createStateTransition(step3, ExitStatus.FAILED.getExitCode(), "end2"));
		transitions.add(StateTransition.createStateTransition(step3, ExitStatus.COMPLETED.getExitCode(), "end3"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end2")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end3")));
		flow.setStateTransitions(transitions);

		job.setFlow(flow);
		try {
			job.execute(jobExecution);
		}
		finally {

			assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
			assertEquals(1, jobExecution.getStepExecutions().size());

			assertEquals(1, jobExecution.getAllFailureExceptions().size());
			assertEquals("Foo", jobExecution.getAllFailureExceptions().get(0).getCause().getCause().getMessage());

		}
	}

	@Test
	void testGetStepExists() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();

		Step step = job.getStep("step2");
		assertNotNull(step);
		assertEquals("step2", step.getName());
	}

	@Test
	void testGetStepExistsWithPrefix() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState("job.step", new StubStep("step")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.setName(flow.getName());
		job.afterPropertiesSet();

		Step step = job.getStep("step");
		assertNotNull(step);
		assertEquals("step", step.getName());
	}

	@Test
	void testGetStepNamesWithPrefix() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState("job.step", new StubStep("step")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.setName(flow.getName());
		job.afterPropertiesSet();

		assertEquals("[step]", job.getStepNames().toString());
	}

	@Test
	void testGetStepNotExists() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();

		Step step = job.getStep("foo");
		assertNull(step);
	}

	@Test
	void testGetStepNotStepState() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();

		Step step = job.getStep("end0");
		assertNull(step);
	}

	@Test
	void testGetStepNestedFlow() throws Exception {
		SimpleFlow nested = new SimpleFlow("nested");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		nested.setStateTransitions(transitions);
		nested.afterPropertiesSet();

		SimpleFlow flow = new SimpleFlow("job");
		transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "nested"));
		transitions.add(StateTransition.createStateTransition(new FlowState(nested, "nested"), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();
		job.setFlow(flow);
		job.afterPropertiesSet();

		List<String> names = new ArrayList<>(job.getStepNames());
		Collections.sort(names);
		assertEquals("[step1, step2]", names.toString());
	}

	@Test
	void testGetStepSplitFlow() throws Exception {
		SimpleFlow flow = new SimpleFlow("job");
		SimpleFlow flow1 = new SimpleFlow("flow1");
		SimpleFlow flow2 = new SimpleFlow("flow2");

		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow1.setStateTransitions(new ArrayList<>(transitions));
		flow1.afterPropertiesSet();
		transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step2")), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow2.setStateTransitions(new ArrayList<>(transitions));
		flow2.afterPropertiesSet();

		transitions = new ArrayList<>();
		transitions.add(StateTransition
			.createStateTransition(new SplitState(Arrays.<Flow>asList(flow1, flow2), "split"), "end2"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end2")));
		flow.setStateTransitions(transitions);
		flow.afterPropertiesSet();

		job.setFlow(flow);
		job.afterPropertiesSet();
		List<String> names = new ArrayList<>(job.getStepNames());
		Collections.sort(names);
		assertEquals("[step1, step2]", names.toString());
	}

	/**
	 * /**
	 *
	 * @author Dave Syer
	 *
	 */
	private class StubStep extends StepSupport {

		private StubStep(String name) {
			super(name);
		}

		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException {
			stepExecution.setStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}

	}

	private StepExecution getStepExecution(JobExecution jobExecution, String stepName) {
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			if (stepExecution.getStepName().equals(stepName)) {
				return stepExecution;
			}
		}
		fail("No stepExecution found with name: [" + stepName + "]");
		return null;
	}

	private void checkRepository(BatchStatus status, ExitStatus exitStatus) {
		JobInstance jobInstance = this.jobExecution.getJobInstance();
		JobExecution other = this.jobExplorer.getJobExecutions(jobInstance).get(0);
		assertEquals(jobInstance.getId(), other.getJobId());
		assertEquals(status, other.getStatus());
		if (exitStatus != null) {
			assertEquals(exitStatus.getExitCode(), other.getExitStatus().getExitCode());
		}
	}

}
