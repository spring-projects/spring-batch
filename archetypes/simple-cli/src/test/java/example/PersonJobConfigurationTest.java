/*
 * Copyright 2006-2013 the original author or authors.
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
package example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>
 * Test cases asserting on the example job's configuration.
 * </p>
 */
@ContextConfiguration(locations = "/test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class PersonJobConfigurationTest {
	@Autowired
	private JobLauncher jobLauncher;
	
	@Autowired
	private Job job;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

    /**
     * <p>
     * Creates a new {@link JobExecution} using a {@link JobLauncher}.
     * </p>
     *
     * @throws Exception if any {@link Exception}'s occur
     */
	@Test
	public void testLaunchJobWithJobLauncher() throws Exception {
		final JobExecution jobExecution = jobLauncher.run(job, new JobParameters());
		assertEquals("Batch status not COMPLETED", BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	/**
	 * <p>
     * Create a unique job instance and check it's execution completes successfully.
     * Uses the convenience methods provided by the testing superclass.
     * </p>
     *
     * @throws Exception if any {@link Exception}'s occur
	 */
	@Test
	public void testLaunchJob() throws Exception {
		final JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobLauncherTestUtils.getUniqueJobParameters());
		assertEquals("Batch status not COMPLETED", BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	/**
     * <p>
	 * Execute a fresh {@link JobInstance} using {@link JobOperator} which is closer to
	 * a remote invocation scenario.
     * </p>
     *
     * @throws Exception if any {@link Exception}'s occur
	 */
	@Test
	public void testLaunchByJobOperator() throws Exception {
		final long jobExecutionId = jobOperator.startNextInstance(jobLauncherTestUtils.getJob().getName());

		final String result = jobOperator.getSummary(jobExecutionId);
		assertTrue("Result does not contain status=COMPLETED", result.contains("status=" + BatchStatus.COMPLETED));
	}
}
