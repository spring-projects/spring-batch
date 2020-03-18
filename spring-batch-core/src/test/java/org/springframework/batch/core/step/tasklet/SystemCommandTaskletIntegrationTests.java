/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.batch.core.step.tasklet;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SystemCommandTasklet}.
 */
public class SystemCommandTaskletIntegrationTests {

	private static final Log log = LogFactory.getLog(SystemCommandTaskletIntegrationTests.class);

	private SystemCommandTasklet tasklet;

	private StepExecution stepExecution = new StepExecution("systemCommandStep", new JobExecution(new JobInstance(1L,
			"systemCommandJob"), 1L, new JobParameters(), "configurationName"));

	@Mock
	private JobExplorer jobExplorer;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		initializeTasklet();
		tasklet.afterPropertiesSet();

		tasklet.beforeStep(stepExecution);
	}

	private void initializeTasklet() {
		tasklet = new SystemCommandTasklet();
		tasklet.setEnvironmentParams(null); // inherit from parent process
		tasklet.setWorkingDirectory(null); // inherit from parent process
		tasklet.setSystemProcessExitCodeMapper(new SimpleSystemProcessExitCodeMapper());
		tasklet.setTimeout(5000); // long enough timeout
		tasklet.setTerminationCheckInterval(500);
		tasklet.setCommand("invalid command, change value for successful execution");
		tasklet.setInterruptOnCancel(true);
		tasklet.setTaskExecutor(new SimpleAsyncTaskExecutor());
	}

	/*
	 * Regular usage scenario - successful execution of system command.
	 */
	@Test
	public void testExecute() throws Exception {
		String command = getJavaCommand() + " --version";
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + command);
		RepeatStatus exitStatus = tasklet.execute(stepExecution.createStepContribution(), null);

		assertEquals(RepeatStatus.FINISHED, exitStatus);
	}

	/*
	 * Failed execution scenario - error exit code returned by system command.
	 */
	@Test
	public void testExecuteFailure() throws Exception {
		String command = getJavaCommand() + " org.springframework.batch.sample.tasklet.UnknownClass";
		tasklet.setCommand(command);
		tasklet.setTimeout(200L);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + command);
		try {
			StepContribution contribution = stepExecution.createStepContribution();
			RepeatStatus exitStatus = tasklet.execute(contribution, null);
			assertEquals(RepeatStatus.FINISHED, exitStatus);
			assertEquals(ExitStatus.FAILED, contribution.getExitStatus());
		}
		catch (RuntimeException e) {
			// on some platforms the system call does not return
			assertEquals("Execution of system command did not finish within the timeout", e.getMessage());
		}
	}

	/*
	 * The attempt to execute the system command results in exception
	 */
	@Test(expected = java.util.concurrent.ExecutionException.class)
	public void testExecuteException() throws Exception {
		String command = "non-sense-that-should-cause-exception-when-attempted-to-execute";
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		tasklet.execute(null, null);
	}

	/*
	 * Failed execution scenario - execution time exceeds timeout.
	 */
	@Test
	public void testExecuteTimeout() throws Exception {
		String command = isRunningOnWindows() ?
				"ping 127.0.0.1" :
					"sleep 3";
		tasklet.setCommand(command);
		tasklet.setTimeout(10);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + command);
		try {
			tasklet.execute(null, null);
			fail();
		}
		catch (SystemCommandException e) {
			assertTrue(e.getMessage().contains("did not finish within the timeout"));
		}
	}

	/*
	 * Job interrupted scenario.
	 */
	@Test
	public void testInterruption() throws Exception {
		String command = isRunningOnWindows() ?
				"ping 127.0.0.1" :
					"sleep 5";
		tasklet.setCommand(command);
		tasklet.setTerminationCheckInterval(10);
		tasklet.afterPropertiesSet();

		stepExecution.setTerminateOnly();
		try {
			tasklet.execute(null, null);
			fail();
		}
		catch (JobInterruptedException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("Job interrupted while executing system command"));
			assertTrue(e.getMessage().contains(command));
		}
	}

	/*
	 * Command property value is required to be set.
	 */
	@Test
	public void testCommandNotSet() throws Exception {
		tasklet.setCommand(null);
		try {
			tasklet.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		tasklet.setCommand("");
		try {
			tasklet.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/*
	 * Timeout must be set to non-zero value.
	 */
	@Test
	public void testTimeoutNotSet() throws Exception {
		tasklet.setCommand("not-empty placeholder");
		tasklet.setTimeout(0);
		try {
			tasklet.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/*
	 * Working directory property must point to an existing location and it must
	 * be a directory
	 */
	@Test
	public void testWorkingDirectory() throws Exception {
		File notExistingFile = new File("not-existing-path");
		Assert.state(!notExistingFile.exists(), "not-existing-path does actually exist");

		try {
			tasklet.setWorkingDirectory(notExistingFile.getCanonicalPath());
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		File notDirectory = File.createTempFile(this.getClass().getName(), null);
		Assert.state(notDirectory.exists(), "The file does not exist");
		Assert.state(!notDirectory.isDirectory(), "The file is actually a directory");

		try {
			tasklet.setWorkingDirectory(notDirectory.getCanonicalPath());
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		File directory = notDirectory.getParentFile();
		Assert.state(directory.exists(), "The directory does not exist");
		Assert.state(directory.isDirectory(), "The directory is not a directory");

		// no error expected now
		tasklet.setWorkingDirectory(directory.getCanonicalPath());
	}

	/*
	 * test stopping a tasklet
	 */
	@Test
	public void testStopped() throws Exception {
		initializeTasklet();
		tasklet.setJobExplorer(jobExplorer);
		tasklet.afterPropertiesSet();
		tasklet.beforeStep(stepExecution);

		JobExecution stoppedJobExecution = new JobExecution(stepExecution.getJobExecution());
		stoppedJobExecution.setStatus(BatchStatus.STOPPING);

		when(jobExplorer.getJobExecution(1L)).thenReturn(stepExecution.getJobExecution(), stepExecution.getJobExecution(), stoppedJobExecution);

		String command = isRunningOnWindows() ?
				"ping 127.0.0.1 -n 5" :
					"sleep 15";
		tasklet.setCommand(command);
		tasklet.setTerminationCheckInterval(10);
		tasklet.afterPropertiesSet();

		StepContribution contribution = stepExecution.createStepContribution();
		StepContext stepContext = new StepContext(stepExecution);
		ChunkContext chunkContext = new ChunkContext(stepContext);
		tasklet.execute(contribution, chunkContext);

		assertEquals(ExitStatus.STOPPED.getExitCode(), contribution.getExitStatus().getExitCode());
	}

	private String getJavaCommand() {
		String javaHome = System.getProperty("java.home");
		String fileSeparator = System.getProperty("file.separator");
		StringBuilder command = new StringBuilder();
		command.append(javaHome);
		command.append(fileSeparator);
		command.append("bin");
		command.append(fileSeparator);
		command.append("java");

		if(isRunningOnWindows()) {
			command.append(".exe");
		}

		return command.toString();
	}

	private boolean isRunningOnWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

}
