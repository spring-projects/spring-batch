/*
 * Copyright 2002-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Lucas Ward
 *
 */
@SpringJUnitConfig
public class JobExecutionListenerParserTests {

	public static boolean beforeCalled = false;

	public static boolean afterCalled = false;

	@Autowired
	Job job;

	@Autowired
	JobRepository jobRepository;

	@Test
	void testListeners() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addLong("now", System.currentTimeMillis())
			.toJobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("testJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		job.execute(jobExecution);
		assertTrue(beforeCalled);
		assertTrue(afterCalled);
	}

	public static class TestComponent {

		@BeforeJob
		public void before(JobExecution jobExecution) {
			beforeCalled = true;
		}

		@AfterJob
		public void after() {
			afterCalled = true;
		}

	}

}
