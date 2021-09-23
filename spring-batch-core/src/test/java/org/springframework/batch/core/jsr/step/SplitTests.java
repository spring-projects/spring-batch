/*
 * Copyright 2020-2021 the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.Decider;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Mahmoud Ben Hassine
 */
public class SplitTests {

	private static final Set<BatchStatus> END_STATUSES =
			EnumSet.of(BatchStatus.COMPLETED, BatchStatus.FAILED, BatchStatus.STOPPED);
	private final JobOperator jobOperator = BatchRuntime.getJobOperator();

	@Test
	public void testSplit() {
		// given
		String jobXMLName = "SplitTests-testSplit-context";
		Properties jobParameters = new Properties();

		// when
		long executionId = jobOperator.start(jobXMLName, jobParameters);
		waitFor(executionId, 10);
		JobExecution jobExecution = jobOperator.getJobExecution(executionId);
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
		assertEquals("COMPLETED", jobExecution.getExitStatus());
		assertEquals(5, stepExecutions.size());
	}
	
	@Test
	public void testDecisionAfterSplit() {
		// given
		String jobXMLName = "SplitTests-testDecisionAfterSplit-context";
		Properties jobParameters = new Properties();

		// when
		long executionId = jobOperator.start(jobXMLName, jobParameters);
		waitFor(executionId, 10);
		JobExecution jobExecution = jobOperator.getJobExecution(executionId);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
		assertEquals(4, jobOperator.getStepExecutions(executionId).size());
		assertEquals(2, StepExecutionCountingDecider.previousStepCount);
	}

	private void waitFor(long executionId, int timeoutInSeconds) {
		Instant startTime = Instant.now();
		while (!END_STATUSES.contains(jobOperator.getJobExecution(executionId).getBatchStatus())) {
			if ((Duration.between(Instant.now(), startTime).getSeconds() > timeoutInSeconds)) {
				fail("Job processing did not complete in time");
			}
		}
	}

	public static class StepExecutionCountingDecider implements Decider {

		static int previousStepCount = 0;

		@Override
		public String decide(StepExecution[] executions) {
			previousStepCount = executions.length;
			return "next";
		}
	}

	public static class ExitStatusSettingBatchlet extends AbstractBatchlet {

		@Inject
		JobContext jobContext;

		@Override
		public String process() throws Exception {
			jobContext.setExitStatus("Should be ignored");
			return null;
		}
	}

}
