/*
 * Copyright 2006-2022 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Hyunsang Han
 *
 */
class FlowStepTests {

	private JobRepository jobRepository;

	private JobExecution jobExecution;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcJobRepositoryFactoryBean jobRepositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
		jobRepositoryFactoryBean.setDataSource(embeddedDatabase);
		jobRepositoryFactoryBean.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		jobRepositoryFactoryBean.afterPropertiesSet();
		jobRepository = jobRepositoryFactoryBean.getObject();
		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
	}

	@Test
	void testAfterPropertiesSet() {
		FlowStep step = new FlowStep(jobRepository);
		assertThrows(IllegalStateException.class, step::afterPropertiesSet);
	}

	@Test
	void testDoExecute() throws Exception {

		FlowStep step = new FlowStep(jobRepository);

		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		StepState step2 = new StepState(new StubStep("step2"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);

		step.setFlow(flow);
		step.afterPropertiesSet();

		StepExecution stepExecution = jobExecution.createStepExecution("step");
		jobRepository.add(stepExecution);
		step.execute(stepExecution);

		stepExecution = getStepExecution(jobExecution, "step");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		stepExecution = getStepExecution(jobExecution, "step2");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		assertEquals(3, jobExecution.getStepExecutions().size());

	}

	// BATCH-1620
	@Test
	void testDoExecuteAndFail() throws Exception {

		FlowStep step = new FlowStep(jobRepository);

		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "step2"));
		StepState step2 = new StepState(new StubStep("step2", true));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.FAILED.getExitCode(), "end0"));
		transitions.add(StateTransition.createStateTransition(step2, ExitStatus.COMPLETED.getExitCode(), "end1"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.FAILED, "end0")));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end1")));
		flow.setStateTransitions(transitions);

		step.setFlow(flow);
		step.afterPropertiesSet();

		StepExecution stepExecution = jobExecution.createStepExecution("step");
		jobRepository.add(stepExecution);
		step.execute(stepExecution);

		stepExecution = getStepExecution(jobExecution, "step1");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		stepExecution = getStepExecution(jobExecution, "step2");
		assertEquals(ExitStatus.FAILED, stepExecution.getExitStatus());
		stepExecution = getStepExecution(jobExecution, "step");
		assertEquals(ExitStatus.FAILED, stepExecution.getExitStatus());
		assertEquals(3, jobExecution.getStepExecutions().size());

	}

	@Test
	void testExecuteWithParentContext() throws Exception {

		FlowStep step = new FlowStep(jobRepository);

		SimpleFlow flow = new SimpleFlow("job");
		List<StateTransition> transitions = new ArrayList<>();
		transitions.add(StateTransition.createStateTransition(new StepState(new StubStep("step1")), "end0"));
		transitions.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
		flow.setStateTransitions(transitions);

		step.setFlow(flow);
		step.afterPropertiesSet();

		StepExecution stepExecution = jobExecution.createStepExecution("step");
		stepExecution.getExecutionContext().put("foo", "bar");
		jobRepository.add(stepExecution);
		step.execute(stepExecution);

		stepExecution = getStepExecution(jobExecution, "step");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		stepExecution = getStepExecution(jobExecution, "step1");
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		assertEquals("bar", stepExecution.getExecutionContext().get("foo"));

	}

	/**
	 * @author Dave Syer
	 *
	 */
	private class StubStep extends StepSupport {

		private final boolean fail;

		private StubStep(String name) {
			this(name, false);
		}

		private StubStep(String name, boolean fail) {
			super(name);
			this.fail = fail;
		}

		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException {
			BatchStatus status = BatchStatus.COMPLETED;
			ExitStatus exitStatus = ExitStatus.COMPLETED;
			if (fail) {
				status = BatchStatus.FAILED;
				exitStatus = ExitStatus.FAILED;
			}
			stepExecution.setStatus(status);
			stepExecution.setExitStatus(exitStatus);
			jobRepository.update(stepExecution);
		}

	}

	private StepExecution getStepExecution(JobExecution jobExecution, String stepName) {
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			if (stepExecution.getStepName().equals(stepName)) {
				return stepExecution;
			}
		}
		throw new IllegalStateException("No stepExecution found with name: [" + stepName + "]");
	}

}
