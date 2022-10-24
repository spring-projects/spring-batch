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

package org.springframework.batch.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional test for graceful shutdown. A batch container is started in a new thread,
 * then it's stopped using {@link JobOperator#stop(long)}}.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 *
 */

@SpringJUnitConfig(
		locations = { "/simple-job-launcher-context.xml", "/jobs/infiniteLoopJob.xml", "/job-runner-context.xml" })
class DatabaseShutdownFunctionalTests {

	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	void testLaunchJob() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob();

		Thread.sleep(1000);

		assertEquals(BatchStatus.STARTED, jobExecution.getStatus());
		assertTrue(jobExecution.isRunning());
		assertNotNull(jobExecution.getVersion());

		jobOperator.stop(jobExecution.getId());

		int count = 0;
		while (jobExecution.isRunning() && count <= 10) {
			logger.info("Checking for end time in JobExecution: count=" + count);
			Thread.sleep(100);
			count++;
		}

		assertFalse(jobExecution.isRunning(), "Timed out waiting for job to end.");
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());

	}

}
