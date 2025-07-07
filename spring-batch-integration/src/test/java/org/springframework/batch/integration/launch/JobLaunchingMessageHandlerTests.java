/*
 * Copyright 2008-2023 the original author or authors.
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
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.JobSupport;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = { "/job-execution-context.xml" })
class JobLaunchingMessageHandlerTests {

	JobLaunchRequestHandler messageHandler;

	StubJobLauncher jobLauncher;

	@BeforeEach
	void setUp() {
		jobLauncher = new StubJobLauncher();
		messageHandler = new JobLaunchingMessageHandler(jobLauncher);
	}

	@Test
	void testSimpleDelivery() throws Exception {
		messageHandler.launch(new JobLaunchRequest(new JobSupport("testjob"), null));

		assertEquals(1, jobLauncher.jobs.size(), "Wrong job count");
		assertEquals("testjob", jobLauncher.jobs.get(0).getName(), "Wrong job name");

	}

	private static class StubJobLauncher implements JobLauncher {

		List<Job> jobs = new ArrayList<>();

		List<JobParameters> parameters = new ArrayList<>();

		AtomicLong jobId = new AtomicLong();

		@Override
		public JobExecution run(Job job, JobParameters jobParameters) {
			jobs.add(job);
			parameters.add(jobParameters);
			return new JobExecution(new JobInstance(jobId.getAndIncrement(), job.getName()), jobParameters);
		}

	}

}
