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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Functional test for graceful shutdown.  A batch container is started in a new thread,
 * then it's stopped using {@link JobExecution#stop()}.  
 * 
 * @author Lucas Ward
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/infiniteLoopJob.xml", "/job-runner-context.xml" })
public class DatabaseShutdownFunctionalTests {
	
	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobOperator jobOperator;
	
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
		
	@Test
	public void testLaunchJob() throws Exception {

		JobExecution jobExecution = jobLauncherTestUtils.launchJob();
		
		Thread.sleep(1000);

		assertEquals(BatchStatus.STARTED, jobExecution.getStatus());
		assertTrue(jobExecution.isRunning());
		assertNotNull(jobExecution.getVersion());

		jobOperator.stop(jobExecution.getId());
		
		int count = 0;
		while(jobExecution.isRunning() && count <= 10){
			logger.info("Checking for end time in JobExecution: count="+count);
			Thread.sleep(100);
			count++;
		}
		
		assertFalse("Timed out waiting for job to end.", jobExecution.isRunning());
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());

	}
	
}
