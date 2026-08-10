/*
 * Copyright 2008-present the original author or authors.
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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml",
		"/org/springframework/batch/samples/restart/stop/stopRestartSample.xml" })
class JobOperatorFunctionalTests {

	private static final Log LOG = LogFactory.getLog(JobOperatorFunctionalTests.class);

	@Autowired
	private JobOperator operator;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Job job;

	@Test
	void testStartStopResumeJob() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addLong("jobOperatorTestParam", 7L).toJobParameters();

		LOG.info("Starting job with parameters: " + jobParameters);
		JobExecution execution = operator.start(job, jobParameters);
		stopAndCheckStatus(execution);

		LOG.info("Restarting job execution with id: " + execution.getId());
		JobExecution resumedExecution = operator.restart(execution);
		assertEquals(execution.getJobParameters(), resumedExecution.getJobParameters());
		stopAndCheckStatus(resumedExecution);

		List<JobInstance> instances = jobRepository.getJobInstances(job.getName(), 0, 1);
		assertEquals(1, instances.size());
		JobInstance instance = instances.get(0);

		List<JobExecution> executions = jobRepository.getJobExecutions(instance);
		assertEquals(2, executions.size());
		// latest execution is the first in the returned list
		assertEquals(resumedExecution.getId(), executions.get(0).getId());
		assertEquals(execution.getId(), executions.get(1).getId());
	}

	private void stopAndCheckStatus(JobExecution execution) throws Exception {
		// wait to the job to get up and running
		Thread.sleep(1000);

		Set<JobExecution> runningExecutions = jobRepository.findRunningJobExecutions(job.getName());
		assertTrue(runningExecutions.contains(execution));

		LOG.info("Stopping job execution with id: " + execution.getId());
		operator.stop(execution);

		int count = 0;
		while (jobRepository.findRunningJobExecutions(job.getName()).contains(execution) && count <= 10) {
			LOG.info("Checking for running JobExecution: count=" + count);
			Thread.sleep(100);
			count++;
		}

		runningExecutions = jobRepository.findRunningJobExecutions(job.getName());
		assertFalse(runningExecutions.contains(execution));
	}

}
