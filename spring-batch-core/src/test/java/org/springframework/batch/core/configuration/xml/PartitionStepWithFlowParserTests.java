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
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Josh Long
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig
class PartitionStepWithFlowParserTests {

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("nameStoringTasklet")
	private NameStoringTasklet nameStoringTasklet;

	@Autowired
	private JobRepository jobRepository;

	private final List<String> savedStepNames = new ArrayList<>();

	@BeforeEach
	void setUp() {
		nameStoringTasklet.setStepNamesList(savedStepNames);
	}

	@Test
	void testRepeatedFlowStep() throws Exception {
		assertNotNull(job1);
		JobParameters jobParameters = new JobParametersBuilder().addLong("gridSize", 1L).toJobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job1.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		job1.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		Collections.sort(savedStepNames);
		assertEquals("[s2, s2, s3, s3]", savedStepNames.toString());
		List<String> stepNames = getStepNames(jobExecution);
		assertEquals(8, stepNames.size());
		assertEquals("[s1, s1, s1:partition0, s1:partition1, s2, s2, s3, s3]", stepNames.toString());
	}

	private List<String> getStepNames(JobExecution jobExecution) {
		List<String> list = new ArrayList<>();
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			list.add(stepExecution.getStepName());
		}
		Collections.sort(list);
		return list;
	}

	public static class Decider implements JobExecutionDecider {

		int count = 0;

		@Override
		public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
			if (count++ < 2) {
				return new FlowExecutionStatus("OK");
			}
			return new FlowExecutionStatus("END");
		}

	}

}
