/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.core.jsr.job.flow.support;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.StateSupport;
import org.springframework.batch.core.job.flow.support.JobFlowExecutorSupport;
import org.springframework.batch.core.job.flow.support.SimpleFlowTests;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.lang.Nullable;

public class JsrFlowTests extends SimpleFlowTests {

	@Override
	@Before
	public void setUp() {
		flow = new JsrFlow("flow1");
	}

	@Test
	public void testNextBasedOnBatchStatus() throws Exception {
		StepExecution stepExecution = new StepExecution("step1", new JobExecution(5L));
		stepExecution.setExitStatus(new ExitStatus("unmapped exit code"));
		stepExecution.setStatus(BatchStatus.FAILED);
		executor = new FlowExecutor(stepExecution);

		State startState = new StateSupport("step1", new FlowExecutionStatus("unmapped exit code"));
		State endState = new StateSupport("failed", FlowExecutionStatus.FAILED);

		StateTransition failureTransition = StateTransition.createStateTransition(startState, "FAILED", "failed");
		StateTransition endTransition = StateTransition.createEndStateTransition(endState);
		flow.setStateTransitions(collect(failureTransition, endTransition));
		flow.afterPropertiesSet();
		FlowExecution execution = flow.start(executor);
		assertEquals(FlowExecutionStatus.FAILED, execution.getStatus());
		assertEquals("failed", execution.getName());
	}

	public static class FlowExecutor extends JobFlowExecutorSupport {

		private StepExecution stepExecution;

		public FlowExecutor(StepExecution stepExecution) {
			this.stepExecution = stepExecution;
		}

		@Nullable
		@Override
		public StepExecution getStepExecution() {
			return stepExecution;
		}
	}
}
