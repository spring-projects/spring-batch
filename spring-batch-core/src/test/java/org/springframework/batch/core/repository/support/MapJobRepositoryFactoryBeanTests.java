/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.batch.core.repository.support;

import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;

import java.util.Date;

import static org.junit.Assert.fail;

/**
 * Tests for {@link MapJobRepositoryFactoryBean}.
 */
public class MapJobRepositoryFactoryBeanTests {

	private MapJobRepositoryFactoryBean tested = new MapJobRepositoryFactoryBean();

	/**
	 * Use the factory to create repository and check the repository remembers
	 * created executions.
	 */
	@Test
	public void testCreateRepository() throws Exception {
		tested.afterPropertiesSet();
		JobRepository repository = tested.getObject();
		Job job = new JobSupport("jobName");
		JobParameters jobParameters = new JobParameters();

		JobExecution jobExecution = repository.createJobExecution(job.getName(), jobParameters);

		// simulate a running execution
		jobExecution.setStartTime(new Date());
		repository.update(jobExecution);

		try {
			repository.createJobExecution(job.getName(), jobParameters);
			fail("Expected JobExecutionAlreadyRunningException");
		}
		catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
	}
}
