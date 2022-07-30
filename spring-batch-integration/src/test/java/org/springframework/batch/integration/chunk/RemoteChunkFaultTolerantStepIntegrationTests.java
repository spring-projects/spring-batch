/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class RemoteChunkFaultTolerantStepIntegrationTests {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Autowired
	private PollableChannel replies;

	@BeforeEach
	void drain() {
		Message<?> message = replies.receive(100L);
		while (message != null) {
			// System.err.println(message);
			message = replies.receive(100L);
		}
	}

	@Test
	void testFailedStep() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job,
				new JobParameters(Collections.singletonMap("item.three", new JobParameter("unsupported"))));
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		// In principle the write count could be more than 2 and less than 9...
		assertEquals(7, stepExecution.getWriteCount());
	}

	@Test
	void testFailedStepOnError() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job,
				new JobParameters(Collections.singletonMap("item.three", new JobParameter("error"))));
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		// In principle the write count could be more than 2 and less than 9...
		assertEquals(7, stepExecution.getWriteCount());
	}

	@Test
	void testSunnyDayFaultTolerant() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job,
				new JobParameters(Collections.singletonMap("item.three", new JobParameter("3"))));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(9, stepExecution.getWriteCount());
	}

	@Test
	void testSkipsInWriter() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job,
				new JobParametersBuilder().addString("item.three", "fail").addLong("run.id", 1L).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(7, stepExecution.getWriteCount());
		// The whole chunk gets skipped...
		assertEquals(2, stepExecution.getWriteSkipCount());
	}

}
