/*
 * Copyright 2008-2025 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.batch.integration.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.integration.JobSupport;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = { "/job-execution-context.xml" })
class JobLaunchingMessageHandlerTests {

	JobLaunchRequestHandler messageHandler;

	StubJobOperator jobOperator;

	@BeforeEach
	void setUp() {
		jobOperator = new StubJobOperator();
		messageHandler = new JobLaunchingMessageHandler(jobOperator);
	}

	@Test
	void testSimpleDelivery() throws Exception {
		messageHandler.launch(new JobLaunchRequest(new JobSupport("testjob"), null));

		assertEquals(1, jobOperator.jobs.size(), "Wrong job count");
		assertEquals("testjob", jobOperator.jobs.get(0).getName(), "Wrong job name");

	}

	private static class StubJobOperator extends TaskExecutorJobOperator {

		List<Job> jobs = new ArrayList<>();

		List<JobParameters> parameters = new ArrayList<>();

		AtomicLong jobId = new AtomicLong();

		@Override
		public JobExecution start(Job job, JobParameters jobParameters) {
			jobs.add(job);
			parameters.add(jobParameters);
			return new JobExecution(new JobInstance(jobId.getAndIncrement(), job.getName()), jobParameters);
		}

	}

}
