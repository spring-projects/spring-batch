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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.util.ClassUtils;

/**
 * @author Lucas Ward
 * 
 */
public class CommandLineJobRunnerTests {

	private String jobPath = ClassUtils.addResourcePathToPackagePath(CommandLineJobRunnerTests.class,
			"launcher-with-environment.xml");

	private String jobName = "test-job";

	private String jobKey = "job.Key=myKey";

	private String scheduleDate = "schedule.Date=01/23/2008";

	private String vendorId = "vendor.id=33243243";

	private String[] args = new String[] { jobPath, jobName, jobKey, scheduleDate, vendorId };

	@Before
	public void setUp() throws Exception {
		JobExecution jobExecution = new JobExecution(null, new Long(1));
		ExitStatus exitStatus = ExitStatus.COMPLETED;
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
	public void testWithJobLocator() {
		jobPath = ClassUtils.addResourcePathToPackagePath(CommandLineJobRunnerTests.class, "launcher-with-locator.xml");
		CommandLineJobRunner.main(new String[] { jobPath, jobName, jobKey });
		assertTrue("Injected JobParametersConverter not used instead of default", StubJobParametersConverter.called);
		assertEquals(0, StubSystemExiter.getStatus());
	}

	@Test
	public void testJobAlreadyRunning() throws Throwable {
		StubJobLauncher.throwExecutionRunningException = true;
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	public void testInvalidArgs() {
		String[] args = new String[] {};
		CommandLineJobRunner.presetSystemExiter(new StubSystemExiter());
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue("Wrong error message: " + errorMessage, errorMessage.contains("Config locations must not be null"));
	}

	@Test
	public void testWrongJobName() {
		String[] args = new String[] { jobPath, "no-such-job" };
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue("Wrong error message: " + errorMessage, errorMessage
				.contains("No bean named 'no-such-job' is defined"));
	}

	@Test
	public void testWithNoParameters() throws Throwable {
		String[] args = new String[] { jobPath, jobName };
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(new JobParameters(), StubJobLauncher.jobParameters);
	}

	@Test
	public void testWithInvalidParameters() throws Throwable {
		String[] args = new String[] { jobPath, jobName, "foo" };
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue("Wrong error message: " + errorMessage, errorMessage
				.contains("in the form name=value"));
	}

	@Test
	public void testRestart() throws Throwable {
		String[] args = new String[] { jobPath, "-restart", jobName };
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		StubJobExplorer.jobInstances = Arrays.asList(new JobInstance(0L, jobParameters, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(jobParameters, StubJobLauncher.jobParameters);
	}

	@Test
	public void testRestartNotFailed() throws Throwable {
		String[] args = new String[] { jobPath, "-restart", jobName };
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		StubJobExplorer.jobInstances = Arrays.asList(new JobInstance(2L, jobParameters, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue("Wrong error message: " + errorMessage, errorMessage
				.contains("No failed or stopped execution found"));
	}

	@Test
	public void testRestartNoParameters() throws Throwable {
		String[] args = new String[] { jobPath, "-restart", jobName };
		StubJobExplorer.jobInstances = new ArrayList<JobInstance>();
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue("Wrong error message: " + errorMessage, errorMessage.contains("No job instance found for job"));
	}

	@Test
	public void testNext() throws Throwable {
		String[] args = new String[] { jobPath, "-next", jobName, "bar=foo" };
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").addString("bar", "foo")
				.toJobParameters();
		StubJobExplorer.jobInstances = Arrays.asList(new JobInstance(2L, jobParameters, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		jobParameters = new JobParametersBuilder().addString("foo", "spam").addString("bar", "foo").toJobParameters();
		assertEquals(jobParameters, StubJobLauncher.jobParameters);
	}

	@Test
	public void testNextFirstInSequence() throws Throwable {
		String[] args = new String[] { jobPath, "-next", jobName };
		StubJobExplorer.jobInstances = new ArrayList<JobInstance>();
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "spam").toJobParameters();
		assertEquals(jobParameters, StubJobLauncher.jobParameters);
	}

	@Test
	public void testNextWithNoParameters() {
		jobPath = ClassUtils.addResourcePathToPackagePath(CommandLineJobRunnerTests.class, "launcher-with-locator.xml");
		CommandLineJobRunner.main(new String[] { jobPath, "-next", "test-job2", jobKey });
		assertEquals(1, StubSystemExiter.getStatus());
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue("Wrong error message: " + errorMessage, errorMessage
				.contains(" No job parameters incrementer found"));
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

		private static int status;

		public void exit(int status) {
			StubSystemExiter.status = status;
		}

		public static int getStatus() {
			return status;
		}
	}

	public static class StubJobLauncher implements JobLauncher {

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

	public static class StubJobExplorer implements JobExplorer {

		static List<JobInstance> jobInstances = new ArrayList<JobInstance>();

		public Set<JobExecution> findRunningJobExecutions(String jobName) {
			throw new UnsupportedOperationException();
		}

		public JobExecution getJobExecution(Long executionId) {
			throw new UnsupportedOperationException();
		}

		public List<JobExecution> getJobExecutions(JobInstance jobInstance) {
			if (jobInstance.getId() == 0) {
				return Arrays.asList(createJobInstance(jobInstance, BatchStatus.FAILED));
			}
			if (jobInstance.getId() == 1) {
				return null;
			}
			return Arrays.asList(createJobInstance(jobInstance, BatchStatus.COMPLETED));
		}

		private JobExecution createJobInstance(JobInstance jobInstance, BatchStatus status) {
			JobExecution jobExecution = new JobExecution(jobInstance, 1L);
			jobExecution.setStatus(status);
			jobExecution.setStartTime(new Date());
			jobExecution.setEndTime(new Date());
			return jobExecution;
		}

		public JobInstance getJobInstance(Long instanceId) {
			throw new UnsupportedOperationException();
		}

		public List<JobInstance> getJobInstances(String jobName, int start, int count) {
			if (jobInstances == null) {
				return new ArrayList<JobInstance>();
			}
			List<JobInstance> result = jobInstances;
			jobInstances = null;
			return result;
		}

		public StepExecution getStepExecution(Long jobExecutionId, Long stepExecutionId) {
			throw new UnsupportedOperationException();
		}
		
		public List<String> getJobNames() {
			throw new UnsupportedOperationException();
		}

	}

	public static class StubJobParametersConverter implements JobParametersConverter {

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
