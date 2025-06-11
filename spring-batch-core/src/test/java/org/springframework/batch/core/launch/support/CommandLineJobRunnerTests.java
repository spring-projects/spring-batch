/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
@Disabled("Disabled until we replace the stub batch infrastructure with a JDBC one")
class CommandLineJobRunnerTests {

	private String jobPath = ClassUtils.addResourcePathToPackagePath(CommandLineJobRunnerTests.class,
			"launcher-with-environment.xml");

	private final String jobName = "test-job";

	private final String jobKey = "job.Key=myKey";

	private final String scheduleDate = "schedule.Date=01/23/2008";

	private final String vendorId = "vendor.id=33243243";

	private final String[] args = new String[] { jobPath, jobName, jobKey, scheduleDate, vendorId };

	private InputStream stdin;

	@BeforeEach
	void setUp() {
		JobExecution jobExecution = new JobExecution(null, 1L, null);
		ExitStatus exitStatus = ExitStatus.COMPLETED;
		jobExecution.setExitStatus(exitStatus);
		StubJobLauncher.jobExecution = jobExecution;
		stdin = System.in;
		System.setIn(new InputStream() {
			@Override
			public int read() {
				return -1;
			}
		});
	}

	@AfterEach
	void tearDown() {
		System.setIn(stdin);
		StubJobLauncher.tearDown();
	}

	@Test
	void testMain() throws Exception {
		CommandLineJobRunner.main(args);
		assertTrue(StubJobParametersConverter.called, "Injected JobParametersConverter not used instead of default");
		assertEquals(0, StubSystemExiter.getStatus());
	}

	@Test
	void testWithJobLocator() throws Exception {
		jobPath = ClassUtils.addResourcePathToPackagePath(CommandLineJobRunnerTests.class, "launcher-with-locator.xml");
		CommandLineJobRunner.main(new String[] { jobPath, jobName, jobKey });
		assertTrue(StubJobParametersConverter.called, "Injected JobParametersConverter not used instead of default");
		assertEquals(0, StubSystemExiter.getStatus());
	}

	@Test
	void testJobAlreadyRunning() throws Throwable {
		StubJobLauncher.throwExecutionRunningException = true;
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	void testInvalidArgs() throws Exception {
		String[] args = new String[] {};
		CommandLineJobRunner.presetSystemExiter(new StubSystemExiter());
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue(errorMessage.contains("At least 2 arguments are required: JobPath/JobClass and jobIdentifier."),
				"Wrong error message: " + errorMessage);
	}

	@Test
	void testWrongJobName() throws Exception {
		String[] args = new String[] { jobPath, "no-such-job" };
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue(
				errorMessage.contains("No bean named 'no-such-job' is defined")
						|| errorMessage.contains("No bean named 'no-such-job' available"),
				"Wrong error message: " + errorMessage);
	}

	@Test
	void testWithNoParameters() throws Throwable {
		String[] args = new String[] { jobPath, jobName };
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(new JobParameters(), StubJobLauncher.jobParameters);
	}

	@Test
	void testWithInvalidStdin() throws Throwable {
		System.setIn(new InputStream() {
			@Override
			public int available() throws IOException {
				throw new IOException("Planned");
			}

			@Override
			public int read() {
				return -1;
			}
		});
		CommandLineJobRunner.main(new String[] { jobPath, jobName });
		assertEquals(0, StubSystemExiter.status);
		assertEquals(0, StubJobLauncher.jobParameters.getParameters().size());
	}

	@Test
	void testWithStdinCommandLine() throws Throwable {
		System.setIn(new InputStream() {
			final char[] input = (jobPath + "\n" + jobName + "\nfoo=bar\nspam=bucket").toCharArray();

			int index = 0;

			@Override
			public int available() {
				return input.length - index;
			}

			@Override
			public int read() {
				return index < input.length - 1 ? (int) input[index++] : -1;
			}
		});
		CommandLineJobRunner.main(new String[0]);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(2, StubJobLauncher.jobParameters.getParameters().size());
	}

	@Test
	void testWithStdinCommandLineWithEmptyLines() throws Throwable {
		System.setIn(new InputStream() {
			final char[] input = (jobPath + "\n" + jobName + "\nfoo=bar\n\nspam=bucket\n\n").toCharArray();

			int index = 0;

			@Override
			public int available() {
				return input.length - index;
			}

			@Override
			public int read() {
				return index < input.length - 1 ? (int) input[index++] : -1;
			}
		});
		CommandLineJobRunner.main(new String[0]);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(2, StubJobLauncher.jobParameters.getParameters().size());
	}

	@Test
	void testWithStdinParameters() throws Throwable {
		String[] args = new String[] { jobPath, jobName };
		System.setIn(new InputStream() {
			final char[] input = "foo=bar\nspam=bucket".toCharArray();

			int index = 0;

			@Override
			public int available() {
				return input.length - index;
			}

			@Override
			public int read() {
				return index < input.length - 1 ? (int) input[index++] : -1;
			}
		});
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(2, StubJobLauncher.jobParameters.getParameters().size());
	}

	@Test
	void testWithInvalidParameters() throws Throwable {
		String[] args = new String[] { jobPath, jobName, "foo" };
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue(errorMessage.contains("in the form name=value"), "Wrong error message: " + errorMessage);
	}

	@Test
	void testStop() throws Throwable {
		String[] args = new String[] { jobPath, "-stop", jobName };
		StubJobExplorer.jobInstances = List.of(new JobInstance(3L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	void testStopFailed() throws Throwable {
		String[] args = new String[] { jobPath, "-stop", jobName };
		StubJobExplorer.jobInstances = List.of(new JobInstance(0L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	void testStopFailedAndRestarted() throws Throwable {
		String[] args = new String[] { jobPath, "-stop", jobName };
		StubJobExplorer.jobInstances = List.of(new JobInstance(5L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	void testStopRestarted() throws Throwable {
		String[] args = new String[] { jobPath, "-stop", jobName };
		JobInstance jobInstance = new JobInstance(3L, jobName);
		StubJobExplorer.jobInstances = List.of(jobInstance);
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	void testAbandon() throws Throwable {
		String[] args = new String[] { jobPath, "-abandon", jobName };
		StubJobExplorer.jobInstances = List.of(new JobInstance(2L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
	}

	@Test
	void testAbandonRunning() throws Throwable {
		String[] args = new String[] { jobPath, "-abandon", jobName };
		StubJobExplorer.jobInstances = List.of(new JobInstance(3L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	void testAbandonAbandoned() throws Throwable {
		String[] args = new String[] { jobPath, "-abandon", jobName };
		StubJobExplorer.jobInstances = List.of(new JobInstance(4L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
	}

	@Test
	void testRestart() throws Throwable {
		String[] args = new String[] { jobPath, "-restart", jobName };
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobInstance jobInstance = new JobInstance(0L, jobName);
		StubJobExplorer.jobInstances = List.of(jobInstance);
		StubJobExplorer.jobParameters = jobParameters;
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(jobParameters, StubJobLauncher.jobParameters);
		StubJobExplorer.jobParameters = new JobParameters();
	}

	@Test
	void testRestartExecution() throws Throwable {
		String[] args = new String[] { jobPath, "-restart", "11" };
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobExecution jobExecution = new JobExecution(new JobInstance(0L, jobName), 11L, jobParameters);
		jobExecution.setStatus(BatchStatus.FAILED);
		StubJobExplorer.jobExecution = jobExecution;
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		assertEquals(jobParameters, StubJobLauncher.jobParameters);
	}

	@Test
	void testRestartExecutionNotFailed() throws Throwable {
		String[] args = new String[] { jobPath, "-restart", "11" };
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobExecution jobExecution = new JobExecution(new JobInstance(0L, jobName), 11L, jobParameters);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		StubJobExplorer.jobExecution = jobExecution;
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		assertNull(StubJobLauncher.jobParameters);
	}

	@Test
	void testRestartNotFailed() throws Throwable {
		String[] args = new String[] { jobPath, "-restart", jobName };
		StubJobExplorer.jobInstances = List.of(new JobInstance(123L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue(errorMessage.contains("No failed or stopped execution found"),
				"Wrong error message: " + errorMessage);
	}

	@Test
	void testNext() throws Throwable {
		String[] args = new String[] { jobPath, "-next", jobName, "bar=foo" };
		StubJobExplorer.jobInstances = List.of(new JobInstance(2L, jobName));
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "spam")
			.addString("bar", "foo")
			.toJobParameters();
		assertEquals(jobParameters, StubJobLauncher.jobParameters);
	}

	@Test
	void testNextFirstInSequence() throws Throwable {
		String[] args = new String[] { jobPath, "-next", jobName };
		StubJobExplorer.jobInstances = new ArrayList<>();
		CommandLineJobRunner.main(args);
		assertEquals(0, StubSystemExiter.status);
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "spam").toJobParameters();
		assertEquals(jobParameters, StubJobLauncher.jobParameters);
	}

	@Test
	void testNextWithNoParameters() throws Exception {
		jobPath = ClassUtils.addResourcePathToPackagePath(CommandLineJobRunnerTests.class, "launcher-with-locator.xml");
		CommandLineJobRunner.main(new String[] { jobPath, "-next", "test-job2", jobKey });
		assertEquals(1, StubSystemExiter.getStatus());
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue(errorMessage.contains(" No job parameters incrementer found"),
				"Wrong error message: " + errorMessage);
	}

	@Test
	void testDestroyCallback() throws Throwable {
		String[] args = new String[] { jobPath, jobName };
		CommandLineJobRunner.main(args);
		assertTrue(StubJobLauncher.destroyed);
	}

	@Test
	void testJavaConfig() throws Exception {
		String[] args = new String[] {
				"org.springframework.batch.core.launch.support.CommandLineJobRunnerTests$Configuration1",
				"invalidJobName" };
		CommandLineJobRunner.presetSystemExiter(new StubSystemExiter());
		CommandLineJobRunner.main(args);
		assertEquals(1, StubSystemExiter.status);
		String errorMessage = CommandLineJobRunner.getErrorMessage();
		assertTrue(errorMessage.contains("A JobLauncher must be provided.  Please add one to the configuration."),
				"Wrong error message: " + errorMessage);
	}

	public static class StubSystemExiter implements SystemExiter {

		private static int status;

		@Override
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

		@Override
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

	public static class StubJobRepository extends JobRepositorySupport {

	}

	public static class StubJobExplorer implements JobExplorer {

		static List<JobInstance> jobInstances = new ArrayList<>();

		static JobExecution jobExecution;

		static JobParameters jobParameters = new JobParameters();

		@Override
		public Set<JobExecution> findRunningJobExecutions(@Nullable String jobName) {
			return new HashSet<>();
		}

		@Nullable
		@Override
		public JobExecution getJobExecution(@Nullable Long executionId) {
			if (jobExecution != null) {
				return jobExecution;
			}
			throw new UnsupportedOperationException();
		}

		@Override
		public List<JobExecution> getJobExecutions(JobInstance jobInstance) {
			if (jobInstance.getId() == 0) {
				return List.of(createJobExecution(jobInstance, BatchStatus.FAILED));
			}
			if (jobInstance.getId() == 1) {
				return null;
			}
			if (jobInstance.getId() == 2) {
				return List.of(createJobExecution(jobInstance, BatchStatus.STOPPED));
			}
			if (jobInstance.getId() == 3) {
				return List.of(createJobExecution(jobInstance, BatchStatus.STARTED));
			}
			if (jobInstance.getId() == 4) {
				return List.of(createJobExecution(jobInstance, BatchStatus.ABANDONED));
			}
			if (jobInstance.getId() == 5) {
				return Arrays.asList(createJobExecution(jobInstance, BatchStatus.STARTED),
						createJobExecution(jobInstance, BatchStatus.FAILED));
			}
			return List.of(createJobExecution(jobInstance, BatchStatus.COMPLETED));
		}

		private JobExecution createJobExecution(JobInstance jobInstance, BatchStatus status) {
			JobExecution jobExecution = new JobExecution(jobInstance, 1L, jobParameters);
			jobExecution.setStatus(status);
			jobExecution.setStartTime(LocalDateTime.now());
			if (status != BatchStatus.STARTED) {
				jobExecution.setEndTime(LocalDateTime.now());
			}
			return jobExecution;
		}

		@Nullable
		@Override
		public JobInstance getJobInstance(@Nullable Long instanceId) {
			throw new UnsupportedOperationException();
		}

		@Nullable
		@Override
		public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
			throw new UnsupportedOperationException();
		}

		@Nullable
		@Override
		public JobInstance getLastJobInstance(String jobName) {
			return null;
		}

		@Nullable
		@Override
		public JobExecution getLastJobExecution(JobInstance jobInstance) {
			return null;
		}

		@Override
		public List<JobInstance> getJobInstances(String jobName, int start, int count) {
			if (jobInstances == null) {
				return new ArrayList<>();
			}
			List<JobInstance> result = jobInstances;
			jobInstances = null;
			return result;
		}

		@Nullable
		@Override
		public StepExecution getStepExecution(@Nullable Long jobExecutionId, @Nullable Long stepExecutionId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> getJobNames() {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("removal")
		@Override
		public List<JobInstance> findJobInstancesByJobName(String jobName, int start, int count) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException {
			long count = 0;

			for (JobInstance jobInstance : jobInstances) {
				if (jobInstance.getJobName().equals(jobName)) {
					count++;
				}
			}

			if (count == 0) {
				throw new NoSuchJobException("Unable to find job instances for " + jobName);
			}
			else {
				return count;
			}
		}

	}

	public static class StubJobParametersConverter implements JobParametersConverter {

		JobParametersConverter delegate = new DefaultJobParametersConverter();

		static boolean called = false;

		@Override
		public JobParameters getJobParameters(@Nullable Properties properties) {
			called = true;
			return delegate.getJobParameters(properties);
		}

		@Override
		public Properties getProperties(@Nullable JobParameters params) {
			throw new UnsupportedOperationException();
		}

	}

	@Configuration
	public static class Configuration1 {

		@Bean
		public String bean1() {
			return "bean1";
		}

	}

}
