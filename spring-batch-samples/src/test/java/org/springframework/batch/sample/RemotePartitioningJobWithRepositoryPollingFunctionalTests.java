/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.sample.config.JobRunnerConfiguration;
import org.springframework.batch.sample.remotepartitioning.polling.MasterConfiguration;
import org.springframework.batch.sample.remotepartitioning.polling.WorkerConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * The master step of the job under test will create 3 partitions for workers
 * to process.
 *
 * @author Mahmoud Ben Hassine
 */
@ContextConfiguration(classes = {JobRunnerConfiguration.class, MasterConfiguration.class})
public class RemotePartitioningJobWithRepositoryPollingFunctionalTests extends RemotePartitioningJobFunctionalTests {

	private AnnotationConfigApplicationContext workerApplicationContext;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		this.workerApplicationContext = new AnnotationConfigApplicationContext(WorkerConfiguration.class);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		this.workerApplicationContext.close();
	}

	@Test
	public void testRemotePartitioningJobWithRepositoryPolling() throws Exception {
		// when
		JobExecution jobExecution = this.jobLauncherTestUtils.launchJob();

		// then
		Assert.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		Assert.assertEquals(4, jobExecution.getStepExecutions().size()); // master + 3 workers
	}

}
