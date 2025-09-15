/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.batch.samples.helloworld;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloWorldJobFunctionalTests {

	@Test
	public void testLaunchJob() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(HelloWorldJobConfiguration.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStepExecutions().iterator().next().getStatus());
	}

}
