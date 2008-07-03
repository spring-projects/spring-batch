package org.springframework.batch.sample.tasklet;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.sample.tasklet.SystemCommandException;
import org.springframework.batch.sample.tasklet.SystemCommandTasklet;
import org.springframework.batch.sample.tasklet.SystemProcessExitCodeMapper;
import org.springframework.util.Assert;

/**
 * Tests for {@link SystemCommandTasklet}.
 */
public class SystemCommandTaskletIntegrationTests extends TestCase {

	private static final Log log = LogFactory.getLog(SystemCommandTaskletIntegrationTests.class);

	private SystemCommandTasklet tasklet = new SystemCommandTasklet();

	private StepExecution stepExecution = new StepExecution("systemCommandStep", new JobExecution(new JobInstance(
			new Long(1), new JobParameters(), "systemCommandJob")));

	protected void setUp() throws Exception {
		tasklet.setEnvironmentParams(null); // inherit from parent process
		tasklet.setWorkingDirectory(null); // inherit from parent process
		tasklet.setSystemProcessExitCodeMapper(new TestExitCodeMapper());
		tasklet.setTimeout(5000); // long enough timeout
		tasklet.setTerminationCheckInterval(500);
		tasklet.setCommand("invalid command, change value for successful execution");
		tasklet.afterPropertiesSet();

		tasklet.beforeStep(stepExecution);
	}

	/**
	 * Regular usage scenario - successful execution of system command.
	 */
	public void testExecute() throws Exception {
		String command = "java -version";
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + command);
		ExitStatus exitStatus = tasklet.execute();

		assertEquals(ExitStatus.FINISHED, exitStatus);
	}

	/**
	 * Failed execution scenario - error exit code returned by system command.
	 */
	public void testExecuteFailure() throws Exception {
		String command = "java org.springframework.batch.sample.tasklet.UnknownClass";
		tasklet.setCommand(command);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + command);
		ExitStatus exitStatus = tasklet.execute();

		assertEquals(ExitStatus.FAILED, exitStatus);
	}

	/**
	 * Failed execution scenario - execution time exceeds timeout.
	 */
	public void testExecuteTimeout() throws Exception {
		String command = "sleep 3";
		tasklet.setCommand(command);
		tasklet.setTimeout(10);
		tasklet.afterPropertiesSet();

		log.info("Executing command: " + command);
		try {
			tasklet.execute();
			fail();
		}
		catch (SystemCommandException e) {
			assertTrue(e.getMessage().indexOf("did not finish successfully within the timeout") > 0);
		}
	}

	/**
	 * Job interrupted scenario.
	 */
	public void testInterruption() throws Exception {
		String command = "sleep 5";
		tasklet.setCommand(command);
		tasklet.setTerminationCheckInterval(10);
		tasklet.afterPropertiesSet();

		stepExecution.setTerminateOnly();
		try {
			tasklet.execute();
			fail();
		}
		catch (JobInterruptedException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf("Job interrupted while executing system command") > -1);
			assertTrue(e.getMessage().indexOf(command) > -1);
		}
	}

	/**
	 * Command property value is required to be set.
	 */
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

	/**
	 * Timeout must be set to non-zero value.
	 */
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

	/**
	 * Working directory property must point to an existing location and it must
	 * be a directory
	 */
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
				return ExitStatus.FINISHED;
			}
			else {
				return ExitStatus.FAILED;
			}
		}

	}

}
