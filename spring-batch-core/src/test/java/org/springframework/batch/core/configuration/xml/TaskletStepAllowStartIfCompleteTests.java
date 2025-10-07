/*
 * Copyright 2013-2025 the original author or authors.
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

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig
class TaskletStepAllowStartIfCompleteTests {

	@Autowired
	Job job;

	@Autowired
	JobRepository jobRepository;

	@Resource
	private ApplicationContext context;

	@Test
	void test() throws Exception {
		// retrieve the step from the context and see that it's allow is set
		AbstractStep abstractStep = context.getBean("simpleJob.step1", AbstractStep.class);
		assertTrue(abstractStep.isAllowStartIfComplete());
	}

	@Disabled
	// FIXME does not seem to be related to the change of parameter conversion
	@Test
	void testRestart() throws Exception {
		JobParametersBuilder paramBuilder = new JobParametersBuilder();
		paramBuilder.addString("value", "foo");
		JobParameters jobParameters = new JobParameters();
		ExecutionContext executionContext = new ExecutionContext();
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);

		job.execute(jobExecution);

		jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		job.execute(jobExecution);

		long count = jobRepository.getStepExecutionCount(jobExecution.getJobInstance(), "simpleJob.step1");
		assertEquals(2, count);
	}

}
