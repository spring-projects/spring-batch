/*
 * Copyright 2013-2022 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
		AbstractStep abstractStep = (AbstractStep) context.getBean("simpleJob.step1");
		assertTrue(abstractStep.isAllowStartIfComplete());
	}

	@Test
	void testRestart() throws Exception {
		JobParametersBuilder paramBuilder = new JobParametersBuilder();
		paramBuilder.addDate("value", new Date());
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), paramBuilder.toJobParameters());

		job.execute(jobExecution);

		jobExecution = jobRepository.createJobExecution(job.getName(), paramBuilder.toJobParameters());
		job.execute(jobExecution);

		int count = jobRepository.getStepExecutionCount(jobExecution.getJobInstance(), "simpleJob.step1");
		assertEquals(2, count);
	}

}
