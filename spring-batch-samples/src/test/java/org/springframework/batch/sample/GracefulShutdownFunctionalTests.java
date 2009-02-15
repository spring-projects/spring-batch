/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Functional test for graceful shutdown. A batch container is started in a new
 * thread, then it's stopped using {@link JobExecution#stop()}.
 * 
 * @author Lucas Ward
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class GracefulShutdownFunctionalTests extends AbstractBatchLauncherTests {

	@Test
	@Override
	public void testLaunchJob() throws Exception {

		final JobParameters jobParameters = new JobParametersBuilder().addLong("timestamp", System.currentTimeMillis())
				.toJobParameters();

		JobExecution jobExecution = getLauncher().run(getJob(), jobParameters);

		Thread.sleep(1000);

		assertEquals(BatchStatus.STARTED, jobExecution.getStatus());
		assertTrue(jobExecution.isRunning());

		jobExecution.stop();

		int count = 0;
		while (jobExecution.isRunning() && count <= 10) {
			logger.info("Checking for end time in JobExecution: count=" + count);
			Thread.sleep(100);
			count++;
		}

		assertFalse("Timed out waiting for job to end.", jobExecution.isRunning());
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());

	}

}
