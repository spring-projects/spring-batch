/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@SpringJUnitConfig
class FlowJobParserTests {

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("job2")
	private Job job2;

	@Autowired
	@Qualifier("job3")
	private Job job3;

	@Autowired
	@Qualifier("job4")
	private Job job4;

	@Autowired
	private JobRepository jobRepository;

	@Test
	void testFlowJob() throws Exception {
		assertNotNull(job1);
		JobExecution jobExecution = jobRepository.createJobExecution(job1.getName(), new JobParameters());
		job1.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(4, stepNames.size());
		assertEquals("[s1, s2, s3, s4]", stepNames.toString());
	}

	@Test
	void testFlowJobWithNestedTransitions() throws Exception {
		assertNotNull(job2);
		JobExecution jobExecution = jobRepository.createJobExecution(job2.getName(), new JobParameters());
		job2.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(3, jobExecution.getStepExecutions().size());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(3, stepNames.size());
		assertEquals("[s2, s3, job2.s1]", stepNames.toString());
	}

	@Test
	void testFlowJobWithNoSteps() throws Exception {
		assertNotNull(job3);
		JobExecution jobExecution = jobRepository.createJobExecution(job3.getName(), new JobParameters());
		job3.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(2, stepNames.size());
		assertEquals("[s2, s3]", stepNames.toString());
	}

	@Test
	void testFlowInSplit() throws Exception {
		assertNotNull(job4);
		JobExecution jobExecution = jobRepository.createJobExecution(job4.getName(), new JobParameters());
		job4.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(4, stepNames.size());
		assertEquals("[s2, s3, s2, s3]", stepNames.toString());
	}

	private List<String> getStepNames(JobExecution jobExecution) {
		List<String> list = new ArrayList<>();
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			list.add(stepExecution.getStepName());
		}
		return list;
	}

}
