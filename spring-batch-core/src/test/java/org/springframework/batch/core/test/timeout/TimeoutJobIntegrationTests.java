/*
 * Copyright 2014-2023 the original author or authors.
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
package org.springframework.batch.core.test.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/META-INF/batch/timeoutJob.xml" })
public class TimeoutJobIntegrationTests {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	@Qualifier("chunkTimeoutJob")
	private Job chunkTimeoutJob;

	@Autowired
	@Qualifier("taskletTimeoutJob")
	private Job taskletTimeoutJob;

	@Test
	void testChunkTimeoutShouldFail() throws Exception {
		JobExecution execution = jobLauncher.run(chunkTimeoutJob,
				new JobParametersBuilder().addLong("id", System.currentTimeMillis()).toJobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	@Test
	void testTaskletTimeoutShouldFail() throws Exception {
		JobExecution execution = jobLauncher.run(taskletTimeoutJob,
				new JobParametersBuilder().addLong("id", System.currentTimeMillis()).toJobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

}
