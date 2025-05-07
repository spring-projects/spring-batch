/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.samples.restart.stop;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/restart/stop/stopRestartSample.xml" })
class JobOperatorFunctionalTests {

	private static final Log LOG = LogFactory.getLog(JobOperatorFunctionalTests.class);

	@Autowired
	private JobOperator operator;

	@Autowired
	private Job job;

	@Test
	void testStartStopResumeJob() throws Exception {
		String params = "jobOperatorTestParam=7,java.lang.Long,true";
		Properties properties = new Properties();
		properties.setProperty("jobOperatorTestParam", "7,java.lang.Long,true");

		long executionId = operator.start(job.getName(), properties);
		assertEquals(params, operator.getParameters(executionId));
		stopAndCheckStatus(executionId);

		long resumedExecutionId = operator.restart(executionId);
		assertEquals(params, operator.getParameters(resumedExecutionId));
		stopAndCheckStatus(resumedExecutionId);

		List<Long> instances = operator.getJobInstances(job.getName(), 0, 1);
		assertEquals(1, instances.size());
		long instanceId = instances.get(0);

		List<Long> executions = operator.getExecutions(instanceId);
		assertEquals(2, executions.size());
		// latest execution is the first in the returned list
		assertEquals(resumedExecutionId, executions.get(0).longValue());
		assertEquals(executionId, executions.get(1).longValue());
	}

	/**
	 * @param executionId id of running job execution
	 */
	private void stopAndCheckStatus(long executionId) throws Exception {
		// wait to the job to get up and running
		Thread.sleep(1000);

		Set<Long> runningExecutions = operator.getRunningExecutions(job.getName());
		assertTrue(runningExecutions.contains(executionId),
				"Wrong executions: " + runningExecutions + " expected: " + executionId);
		assertTrue(operator.getSummary(executionId).contains(BatchStatus.STARTED.toString()),
				"Wrong summary: " + operator.getSummary(executionId));

		operator.stop(executionId);

		int count = 0;
		while (operator.getRunningExecutions(job.getName()).contains(executionId) && count <= 10) {
			LOG.info("Checking for running JobExecution: count=" + count);
			Thread.sleep(100);
			count++;
		}

		runningExecutions = operator.getRunningExecutions(job.getName());
		assertFalse(runningExecutions.contains(executionId),
				"Wrong executions: " + runningExecutions + " expected: " + executionId);
		assertTrue(operator.getSummary(executionId).contains(BatchStatus.STOPPED.toString()),
				"Wrong summary: " + operator.getSummary(executionId));

		// there is just a single step in the test job
		Map<Long, String> summaries = operator.getStepExecutionSummaries(executionId);
		LOG.info(summaries);
		assertTrue(summaries.values().toString().contains(BatchStatus.STOPPED.toString()));
	}

	@Test
	void testMultipleSimultaneousInstances() throws Exception {
		String jobName = job.getName();

		Set<String> names = operator.getJobNames();
		assertEquals(1, names.size());
		assertTrue(names.contains(jobName));

		long exec1 = operator.startNextInstance(jobName);
		long exec2 = operator.startNextInstance(jobName);

		assertTrue(exec1 != exec2);
		assertNotEquals(operator.getParameters(exec1), operator.getParameters(exec2));

		// Give the asynchronous task executor a chance to start executions
		Thread.sleep(1000);

		Set<Long> executions = operator.getRunningExecutions(jobName);
		assertTrue(executions.contains(exec1));
		assertTrue(executions.contains(exec2));

		int count = 0;
		boolean running = operator.getSummary(exec1).contains("STARTED")
				&& operator.getSummary(exec2).contains("STARTED");

		while (count++ < 10 && !running) {
			Thread.sleep(100L);
			running = operator.getSummary(exec1).contains("STARTED") && operator.getSummary(exec2).contains("STARTED");
		}

		assertTrue(running, String.format("Jobs not started: [%s] and [%s]", operator.getSummary(exec1),
				operator.getSummary(exec1)));

		operator.stop(exec1);
		operator.stop(exec2);
	}

}
