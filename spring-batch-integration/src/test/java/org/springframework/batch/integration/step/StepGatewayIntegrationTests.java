/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.integration.step;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 *
 */
@SpringJUnitConfig
class StepGatewayIntegrationTests {

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	@Qualifier("job")
	private Job job;

	@Autowired
	private TestTasklet tasklet;

	@AfterEach
	void clear() {
		tasklet.setFail(false);
	}

	@Test
	void testLaunchJob() throws Exception {
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Test
	void testLaunchFailedJob() throws Exception {
		tasklet.setFail(true);
		JobExecution jobExecution = jobOperator.start(job,
				new JobParametersBuilder().addLong("run.id", 2L).toJobParameters());
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertEquals(ExitStatus.FAILED, jobExecution.getExitStatus());
	}

}
