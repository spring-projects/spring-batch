/*
 * Copyright 2008-2022 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SystemCommandTasklet}.
 */
@ExtendWith(MockitoExtension.class)
class SystemCommandTaskletIntegrationTests {

	private static final Log log = LogFactory.getLog(SystemCommandTaskletIntegrationTests.class);

	private SystemCommandTasklet tasklet;

	private final StepExecution stepExecution = new StepExecution("systemCommandStep",
			new JobExecution(new JobInstance(1L, "systemCommandJob"), 1L, new JobParameters()));

	@Mock
	private JobExplorer jobExplorer;

	@BeforeEach
	void setUp() throws Exception {

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
	void testExecute() throws Exception {
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
	void testExecuteFailure() throws Exception {
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
	@Test
	void testExecuteException() throws Exception {
		String command = "non-sense-that-should-cause-exception-when-attempted-to-execute";
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		assertThrows(java.util.concurrent.ExecutionException.class, () -> tasklet.execute(null, null));
	}

	/*
	 * Failed execution scenario - execution time exceeds timeout.
	 */
	@Test
	void testExecuteTimeout() throws Exception {
		String command = isRunningOnWindows() ? "ping 127.0.0.1" : "sleep 3";
		tasklet.setCommand(command);
		tasklet.setTimeout(10);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + command);
		Exception exception = assertThrows(SystemCommandException.class, () -> tasklet.execute(null, null));
		assertTrue(exception.getMessage().contains("did not finish within the timeout"));
	}

	/*
	 * Job interrupted scenario.
	 */
	@Test
	void testInterruption() throws Exception {
		String command = isRunningOnWindows() ? "ping 127.0.0.1" : "sleep 5";
		tasklet.setCommand(command);
		tasklet.setTerminationCheckInterval(10);
		tasklet.afterPropertiesSet();

		stepExecution.setTerminateOnly();
		Exception exception = assertThrows(JobInterruptedException.class, () -> tasklet.execute(null, null));
		String message = exception.getMessage();
		System.out.println(message);
		assertTrue(message.contains("Job interrupted while executing system command"));
		assertTrue(message.contains(command));
	}

	/*
	 * Command property value is required to be set.
	 */
	@Test
	void testCommandNotSet() {
		tasklet.setCommand(null);
		assertThrows(IllegalArgumentException.class, tasklet::afterPropertiesSet);

		tasklet.setCommand("");
		assertThrows(IllegalArgumentException.class, tasklet::afterPropertiesSet);
	}

	/*
	 * Timeout must be set to non-zero value.
	 */
	@Test
	void testTimeoutNotSet() {
		tasklet.setCommand("not-empty placeholder");
		tasklet.setTimeout(0);
		assertThrows(IllegalArgumentException.class, tasklet::afterPropertiesSet);
	}

	/*
	 * Working directory property must point to an existing location and it must be a
	 * directory
	 */
	@Test
	void testWorkingDirectory() throws Exception {
		File notExistingFile = new File("not-existing-path");
		Assert.state(!notExistingFile.exists(), "not-existing-path does actually exist");

		assertThrows(IllegalArgumentException.class,
				() -> tasklet.setWorkingDirectory(notExistingFile.getCanonicalPath()));

		File notDirectory = File.createTempFile(this.getClass().getName(), null);
		Assert.state(notDirectory.exists(), "The file does not exist");
		Assert.state(!notDirectory.isDirectory(), "The file is actually a directory");

		assertThrows(IllegalArgumentException.class,
				() -> tasklet.setWorkingDirectory(notDirectory.getCanonicalPath()));

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
	void testStopped() throws Exception {
		initializeTasklet();
		tasklet.setJobExplorer(jobExplorer);
		tasklet.afterPropertiesSet();
		tasklet.beforeStep(stepExecution);

		JobExecution stoppedJobExecution = new JobExecution(stepExecution.getJobExecution());
		stoppedJobExecution.setStatus(BatchStatus.STOPPING);

		when(jobExplorer.getJobExecution(1L)).thenReturn(stepExecution.getJobExecution(),
				stepExecution.getJobExecution(), stoppedJobExecution);

		String command = isRunningOnWindows() ? "ping 127.0.0.1 -n 5" : "sleep 15";
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

		if (isRunningOnWindows()) {
			command.append(".exe");
		}

		return command.toString();
	}

	private boolean isRunningOnWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

}
