/*
 * Copyright 2008-2024 the original author or authors.
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
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SystemCommandTasklet}.
 */
@ExtendWith(MockitoExtension.class)
class SystemCommandTaskletIntegrationTests {

	private static final Log log = LogFactory.getLog(SystemCommandTaskletIntegrationTests.class);

	private SystemCommandTasklet tasklet;

	private final StepExecution stepExecution = new StepExecution("systemCommandStep",
			new JobExecution(1L, new JobInstance(1L, "systemCommandJob"), new JobParameters()));

	@Mock
	private JobRepository jobRepository;

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
	 * Power usage scenario - successful execution of system command.
	 */
	@Test
	public void testExecuteWithSeparateArgument() throws Exception {
		tasklet.setCommand(getJavaCommand(), "--version");
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + getJavaCommand() + " --version");
		RepeatStatus exitStatus = tasklet.execute(stepExecution.createStepContribution(), null);

		assertEquals(RepeatStatus.FINISHED, exitStatus);
	}

	/*
	 * Regular usage scenario - successful execution of system command.
	 */
	@Test
	void testExecute() throws Exception {
		String[] command = new String[] { getJavaCommand(), "--version" };
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + String.join(" ", command));
		RepeatStatus exitStatus = tasklet.execute(stepExecution.createStepContribution(), null);

		assertEquals(RepeatStatus.FINISHED, exitStatus);
	}

	/*
	 * Failed execution scenario - error exit code returned by system command.
	 */
	@Test
	void testExecuteFailure() throws Exception {
		String[] command = new String[] { getJavaCommand() + " org.springframework.batch.sample.tasklet.UnknownClass" };
		tasklet.setCommand(command);
		tasklet.setTimeout(200L);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + String.join(" ", command));
		try {
			StepContribution contribution = stepExecution.createStepContribution();
			RepeatStatus exitStatus = tasklet.execute(contribution, null);
			assertEquals(RepeatStatus.FINISHED, exitStatus);
			assertEquals(ExitStatus.FAILED, contribution.getExitStatus());
		}
		catch (Exception e) {
			// on some platforms the system call does not return
			assertTrue(e.getMessage().contains("Cannot run program"));
		}
	}

	/*
	 * The attempt to execute the system command results in exception
	 */
	@Test
	void testExecuteException() throws Exception {
		String[] command = new String[] { "non-sense-that-should-cause-exception-when-attempted-to-execute" };
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		assertThrows(java.util.concurrent.ExecutionException.class, () -> tasklet.execute(null, null));
	}

	/*
	 * Failed execution scenario - execution time exceeds timeout.
	 */
	@Test
	void testExecuteTimeout() throws Exception {
		String[] command = isRunningOnWindows() ? new String[] { "ping", "127.0.0.1" } : new String[] { "sleep", "3" };
		tasklet.setCommand(command);
		tasklet.setTimeout(10);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + String.join(" ", command));
		Exception exception = assertThrows(SystemCommandException.class, () -> tasklet.execute(null, null));
		assertTrue(exception.getMessage().contains("did not finish within the timeout"));
	}

	/*
	 * Job interrupted scenario.
	 */
	@Test
	void testInterruption() throws Exception {
		String[] command = isRunningOnWindows() ? new String[] { "ping", "127.0.0.1" } : new String[] { "sleep", "5" };
		tasklet.setCommand(command);
		tasklet.setTerminationCheckInterval(10);
		tasklet.afterPropertiesSet();

		stepExecution.setTerminateOnly();
		Exception exception = assertThrows(JobInterruptedException.class, () -> tasklet.execute(null, null));
		String message = exception.getMessage();
		assertTrue(message.contains("Job interrupted while executing system command"));
		assertTrue(message.contains(command[0]));
	}

	/*
	 * Command Runner is required to be set.
	 */
	@Test
	public void testCommandRunnerNotSet() {
		tasklet.setCommandRunner(null);
		assertThrows(IllegalStateException.class, tasklet::afterPropertiesSet);
	}

	/*
	 * Command property value is required to be set.
	 */
	@Test
	void testCommandNotSet() {
		tasklet.setCommand();
		assertThrows(IllegalStateException.class, tasklet::afterPropertiesSet);

		tasklet.setCommand((String[]) null);
		assertThrows(IllegalStateException.class, tasklet::afterPropertiesSet);

		tasklet.setCommand("");
		assertThrows(IllegalStateException.class, tasklet::afterPropertiesSet);
	}

	/*
	 * Timeout must be set to non-zero value.
	 */
	@Test
	void testTimeoutNotSet() {
		tasklet.setCommand("not-empty placeholder");
		tasklet.setTimeout(0);
		assertThrows(IllegalStateException.class, tasklet::afterPropertiesSet);
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
		tasklet.setJobRepository(jobRepository);
		tasklet.afterPropertiesSet();
		tasklet.beforeStep(stepExecution);

		JobExecution stoppedJobExecution = stepExecution.getJobExecution();
		stoppedJobExecution.setStatus(BatchStatus.STOPPING);

		when(jobRepository.getJobExecution(1L)).thenReturn(stepExecution.getJobExecution(),
				stepExecution.getJobExecution(), stoppedJobExecution);

		String[] command = isRunningOnWindows() ? new String[] { "ping", "127.0.0.1", "-n", "5" }
				: new String[] { "sleep", "15" };
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

	@Test
	public void testExecuteWithSuccessfulCommandRunnerMockExecution() throws Exception {
		StepContribution stepContribution = stepExecution.createStepContribution();
		CommandRunner commandRunner = mock();
		Process process = mock();
		String[] command = new String[] { "invalid command" };

		when(commandRunner.exec(eq(command), any(), any())).thenReturn(process);
		when(process.waitFor()).thenReturn(0);

		tasklet.setCommandRunner(commandRunner);
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		RepeatStatus exitStatus = tasklet.execute(stepContribution, null);

		assertEquals(RepeatStatus.FINISHED, exitStatus);
		assertEquals(ExitStatus.COMPLETED, stepContribution.getExitStatus());
	}

	@Test
	public void testExecuteWithFailedCommandRunnerMockExecution() throws Exception {
		StepContribution stepContribution = stepExecution.createStepContribution();
		CommandRunner commandRunner = mock();
		Process process = mock();
		String[] command = new String[] { "invalid command" };

		when(commandRunner.exec(eq(command), any(), any())).thenReturn(process);
		when(process.waitFor()).thenReturn(1);

		tasklet.setCommandRunner(commandRunner);
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		Exception exception = assertThrows(SystemCommandException.class, () -> tasklet.execute(stepContribution, null));
		assertTrue(exception.getMessage().contains("failed with exit code"));
		assertEquals(ExitStatus.FAILED, stepContribution.getExitStatus());
	}

}
