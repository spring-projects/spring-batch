package org.springframework.batch.core.step.tasklet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;

/**
 * Tests for {@link SystemCommandTasklet}.
 */
public class SystemCommandTaskletIntegrationTests {

	private static final Log log = LogFactory.getLog(SystemCommandTaskletIntegrationTests.class);

	private SystemCommandTasklet tasklet = new SystemCommandTasklet();

	private StepExecution stepExecution = new StepExecution("systemCommandStep", new JobExecution(new JobInstance(1L,
			new JobParameters(), "systemCommandJob")));

	@Before
	public void setUp() throws Exception {
		tasklet.setEnvironmentParams(null); // inherit from parent process
		tasklet.setWorkingDirectory(null); // inherit from parent process
		tasklet.setSystemProcessExitCodeMapper(new TestExitCodeMapper());
		tasklet.setTimeout(5000); // long enough timeout
		tasklet.setTerminationCheckInterval(500);
		tasklet.setCommand("invalid command, change value for successful execution");
		tasklet.setInterruptOnCancel(true);
		tasklet.setTaskExecutor(new SimpleAsyncTaskExecutor());
		tasklet.afterPropertiesSet();

		tasklet.beforeStep(stepExecution);
	}

	/*
	 * Regular usage scenario - successful execution of system command.
	 */
	@Test
	public void testExecute() throws Exception {
		String command = "java -version";
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
		String command = "java org.springframework.batch.sample.tasklet.UnknownClass";
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
		String command = "sleep 3";
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
		String command = "sleep 5";
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
		Assert.state(!notExistingFile.exists());

		try {
			tasklet.setWorkingDirectory(notExistingFile.getCanonicalPath());
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		File notDirectory = File.createTempFile(this.getClass().getName(), null);
		Assert.state(notDirectory.exists());
		Assert.state(!notDirectory.isDirectory());

		try {
			tasklet.setWorkingDirectory(notDirectory.getCanonicalPath());
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		File directory = notDirectory.getParentFile();
		Assert.state(directory.exists());
		Assert.state(directory.isDirectory());

		// no error expected now
		tasklet.setWorkingDirectory(directory.getCanonicalPath());
	}

	/**
	 * Exit code mapper containing mapping logic expected by the tests. 0 means
	 * finished successfully, other value means failure.
	 */
	private static class TestExitCodeMapper implements SystemProcessExitCodeMapper {

		public ExitStatus getExitStatus(int exitCode) {
			if (exitCode == 0) {
				return ExitStatus.COMPLETED;
			}
			else {
				return ExitStatus.FAILED;
			}
		}

	}

}
