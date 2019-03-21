/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.batch.core.jsr.step;

import java.util.List;
import java.util.Properties;
import javax.batch.api.Decider;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.jsr.AbstractJsrTestCase;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;

public class DecisionStepTests extends AbstractJsrTestCase {

	private static ApplicationContext baseContext;

	private JobExplorer jobExplorer;

	@Before
	public void setUp() {
		StepExecutionCountingDecider.previousStepCount = 0;

		if(jobExplorer == null) {
			baseContext = new GenericXmlApplicationContext("jsrBaseContext.xml");

			baseContext.getAutowireCapableBeanFactory().autowireBeanProperties(this,
					AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		}
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	@Test
	public void testDecisionAsFirstStepOfJob() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionAsFirstStep-context", new Properties(), 10000L);
		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		assertEquals(0, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionThrowsException() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionThrowsException-context", new Properties(), 10000L);
		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		assertEquals(2, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionValidExitStatus() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionValidExitStatus-context", new Properties(), 10000L);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertEquals(3, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionUnmappedExitStatus() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionInvalidExitStatus-context", new Properties(), 10000L);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		List<StepExecution> stepExecutions = BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId());
		assertEquals(2, stepExecutions.size());

		for (StepExecution curExecution : stepExecutions) {
			assertEquals(BatchStatus.COMPLETED, curExecution.getBatchStatus());
		}
	}

	@Test
	public void testDecisionCustomExitStatus() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionCustomExitStatus-context", new Properties(), 10000L);
		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		assertEquals(2, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
		assertEquals("CustomFail", execution.getExitStatus());
	}

	@Test
	public void testDecisionAfterFlow() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionAfterFlow-context", new Properties(), 10000L);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertEquals(3, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionAfterSplit() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionAfterSplit-context", new Properties(), 10000L);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertEquals(4, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
		assertEquals(2, StepExecutionCountingDecider.previousStepCount);
	}

	@Test
	public void testDecisionRestart() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-restart-context", new Properties(), 10000L);
		assertEquals(BatchStatus.STOPPED, execution.getBatchStatus());

		List<StepExecution> stepExecutions = BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId());
		assertEquals(2, stepExecutions.size());

		assertEquals("step1", stepExecutions.get(0).getStepName());
		assertEquals("decision1", stepExecutions.get(1).getStepName());

		JobExecution execution2 = restartJob(execution.getExecutionId(), new Properties(), 10000L);
		assertEquals(BatchStatus.COMPLETED, execution2.getBatchStatus());

		List<StepExecution> stepExecutions2 = BatchRuntime.getJobOperator().getStepExecutions(execution2.getExecutionId());
		assertEquals(2, stepExecutions2.size());

		assertEquals("decision1", stepExecutions2.get(0).getStepName());
		assertEquals("step2", stepExecutions2.get(1).getStepName());
	}

	public static class RestartDecider implements Decider {

		private static int runs = 0;

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			Assert.isTrue(executions.length == 1, "Invalid array length");
			Assert.isTrue(executions[0].getStepName().equals("step1"), "Incorrect step name");

			if(runs == 0) {
				runs++;
				return "STOP_HERE";
			} else {
				return "CONTINUE";
			}
		}
	}

	public static class StepExecutionCountingDecider implements Decider {

		static int previousStepCount = 0;

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			previousStepCount = executions.length;
			return "next";
		}
	}

	public static class NextDecider implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			for(StepExecution stepExecution : executions) {
				if ("customFailTest".equals(stepExecution.getStepName())) {
					return "CustomFail";
				}
			}

			return "next";
		}
	}

	public static class FailureDecider implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			throw new RuntimeException("Expected");
		}
	}
}
