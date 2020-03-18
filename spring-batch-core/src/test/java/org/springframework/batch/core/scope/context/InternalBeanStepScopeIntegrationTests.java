/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.batch.core.scope.context;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * @author mminella
 */
public class InternalBeanStepScopeIntegrationTests {

	@Test
	public void testCommitIntervalJobParameter() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/scope/context/CommitIntervalJobParameter-context.xml");
		Job job = context.getBean(Job.class);
		JobLauncher launcher = context.getBean(JobLauncher.class);

		JobExecution execution = launcher.run(job, new JobParametersBuilder().addLong("commit.interval", 1l).toJobParameters());

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().iterator().next().getReadCount());
		assertEquals(2, execution.getStepExecutions().iterator().next().getWriteCount());
	}

	@Test
	public void testInvalidCommitIntervalJobParameter() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/scope/context/CommitIntervalJobParameter-context.xml");
		Job job = context.getBean(Job.class);
		JobLauncher launcher = context.getBean(JobLauncher.class);

		JobExecution execution = launcher.run(job, new JobParametersBuilder().addLong("commit.intervall", 1l).toJobParameters());

		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}
}
