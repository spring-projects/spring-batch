/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.util.ClassUtils;

/**
 * @author Lucas Ward
 * 
 */
public class CommandLineJobRunnerTests {

	private static final String JOB = ClassUtils.addResourcePathToPackagePath(CommandLineJobRunnerTests.class,
			"job-with-environment.xml");

	private static final String JOB_NAME = "test-job";

	private String jobPath = JOB;

	private String jobName = JOB_NAME;

	private String jobKey = "job.Key=myKey";

	private String scheduleDate = "schedule.Date=01/23/2008";

	private String vendorId = "vendor.id=33243243";

	private String[] args = new String[] { jobPath, jobName, jobKey, scheduleDate, vendorId };

	@Before
	public void setUp() throws Exception {
		JobExecution jobExecution = new JobExecution(null, new Long(1));
		ExitStatus exitStatus = ExitStatus.FINISHED;
		jobExecution.setExitStatus(exitStatus);
		StubJobLauncher.jobExecution = jobExecution;
	}

	@Test
	public void testMain() {
		CommandLineJobRunner.main(args);
		assertTrue("Injected JobParametersConverter not used instead of default", StubJobParametersConverter.called);
		assertEquals(0, StubSystemExiter.getStatus());
	}

	@Test
	public void testJobAlreadyRunning() throws Throwable {
		StubJobLauncher.throwExecutionRunningException = true;
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	// can't test because it will cause the system to exit.
	// public void testInvalidArgs(){
	//		
	// String[] args = new String[]{jobPath, jobName};
	// CommandLineJobRunner.main(args);
	// }

	@Test
	public void testWithNoParameters() throws Throwable {
		String[] args = new String[] { jobPath, jobName };
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(new JobParameters(), StubJobLauncher.jobParameters);
	}

	@Test
	public void testDestroyCallback() throws Throwable {
		String[] args = new String[] { jobPath, jobName };
		CommandLineJobRunner.main(args);
		assertTrue(StubJobLauncher.destroyed);
	}

	@After
	public void tearDown() throws Exception {
		StubJobLauncher.tearDown();
	}

	public static class StubSystemExiter implements SystemExiter {

		public static int status;

		public void exit(int status) {
			StubSystemExiter.status = status;
		}

		public static int getStatus() {
			return status;
		}
	}

	private static class StubJobLauncher implements JobLauncher {

		public static JobExecution jobExecution;

		public static boolean throwExecutionRunningException = false;

		public static JobParameters jobParameters;

		private static boolean destroyed = false;

		public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException {

			StubJobLauncher.jobParameters = jobParameters;

			if (throwExecutionRunningException) {
				throw new JobExecutionAlreadyRunningException("");
			}

			return jobExecution;
		}

		public void destroy() {
			destroyed = true;
		}

		public static void tearDown() {
			jobExecution = null;
			throwExecutionRunningException = false;
			jobParameters = null;
			destroyed = false;
		}
	}

	private static class StubJobParametersConverter implements JobParametersConverter {

		JobParametersConverter delegate = new DefaultJobParametersConverter();

		static boolean called = false;

		public JobParameters getJobParameters(Properties properties) {
			called = true;
			return delegate.getJobParameters(properties);
		}

		public Properties getProperties(JobParameters params) {
			throw new UnsupportedOperationException();
		}

	}

}
