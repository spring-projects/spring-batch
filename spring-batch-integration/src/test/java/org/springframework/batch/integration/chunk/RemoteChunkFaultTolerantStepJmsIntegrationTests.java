/*
 * Copyright 2010-2025 the original author or authors.
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

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileSystemUtils;

@SpringJUnitConfig
@DirtiesContext
class RemoteChunkFaultTolerantStepJmsIntegrationTests {

	@BeforeAll
	static void clear() {
		FileSystemUtils.deleteRecursively(new File("activemq-data"));
	}

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@Test
	void testFailedStep() throws Exception {
		JobExecution jobExecution = jobOperator.start(job,
				new JobParameters(Set.of(new JobParameter<>("item.three", "unsupported", String.class))));
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		// In principle the write count could be more than 2 and less than 9...
		assertEquals(7, stepExecution.getWriteCount());
	}

	@Test
	void testFailedStepOnError() throws Exception {
		JobExecution jobExecution = jobOperator.start(job,
				new JobParameters(Set.of(new JobParameter<>("item.three", "error", String.class))));
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		// In principle the write count could be more than 2 and less than 9...
		assertEquals(7, stepExecution.getWriteCount());
	}

	@Test
	void testSunnyDayFaultTolerant() throws Exception {
		JobExecution jobExecution = jobOperator.start(job,
				new JobParameters(Set.of(new JobParameter("item.three", "3", Integer.class))));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(9, stepExecution.getWriteCount());
	}

	@Test
	void testSkipsInWriter() throws Exception {
		JobExecution jobExecution = jobOperator.start(job,
				new JobParametersBuilder().addString("item.three", "fail").addLong("run.id", 1L).toJobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(7, stepExecution.getWriteCount());
		assertEquals(2, stepExecution.getWriteSkipCount());
	}

}
