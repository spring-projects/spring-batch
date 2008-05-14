package org.springframework.batch.sample.tasklet;

import java.io.File;
import java.io.IOException;

import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link Tasklet} that executes a system command.
 * 
 * The system command is executed in a new thread - timeout value is required to
 * be set, so that the batch job does not hang forever if the external process
 * hangs.
 * 
 * @author Robert Kasanicky
 */
public class SystemCommandTasklet implements Tasklet, InitializingBean {

	private String command;

	private String[] environmentParams = null;

	private File workingDirectory = null;

	private SystemProcessExitCodeMapper systemProcessExitCodeMapper = new SimpleSystemProcessExitCodeMapper();

	private long timeout = 0;

	/**
	 * Execute system command and map its exit code to {@link ExitStatus} using
	 * {@link SystemProcessExitCodeMapper}.
	 */
	public ExitStatus execute() throws Exception {
		ExecutorThread executorThread = new ExecutorThread();
		executorThread.start();
		executorThread.join(timeout);

		if (executorThread.finishedSuccessfully) {
			return systemProcessExitCodeMapper.getExitStatus(executorThread.exitCode);
		}
		else {
			executorThread.interrupt();
			throw new SystemCommandException(
					"Execution of system command failed (did not finish successfully within the timeout)");
		}

	}

	/**
	 * @param command command to be executed in a separate system process
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * @param envp environment parameter values, inherited from parent process
	 * when not set (or set to null).
	 */
	public void setEnvironmentParams(String[] envp) {
		this.environmentParams = envp;
	}

	/**
	 * @param dir working directory of the spawned process, inherited from
	 * parent process when not set (or set to null).
	 */
	public void setWorkingDirectory(String dir) {
		if (dir == null) {
			this.workingDirectory = null;
			return;
		}
		this.workingDirectory = new File(dir);
		Assert.isTrue(workingDirectory.exists(), "working directory must exist");
		Assert.isTrue(workingDirectory.isDirectory(), "working directory value must be a directory");

	}

	public void afterPropertiesSet() throws Exception {
		Assert.hasLength(command, "'command' property value is required");
		Assert.notNull(systemProcessExitCodeMapper, "SystemProcessExitCodeMapper must be set");
		Assert.isTrue(timeout > 0, "timeout value must be greater than zero");
	}

	/**
	 * @param systemProcessExitCodeMapper maps system process return value to
	 * <code>ExitStatus</code> returned by Tasklet.
	 * {@link SimpleSystemProcessExitCodeMapper} is used by default.
	 */
	public void setSystemProcessExitCodeMapper(SystemProcessExitCodeMapper systemProcessExitCodeMapper) {
		this.systemProcessExitCodeMapper = systemProcessExitCodeMapper;
	}

	/**
	 * @param timeout upper limit for how long the execution of the external
	 * program is allowed to last.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Thread that executes the system command.
	 */
	private class ExecutorThread extends Thread {
		volatile int exitCode = -1;

		volatile boolean finishedSuccessfully = false;

		public void run() {
			try {
				Process process = Runtime.getRuntime().exec(command, environmentParams, workingDirectory);
				exitCode = process.waitFor();
				finishedSuccessfully = true;
			}
			catch (IOException e) {
				throw new SystemCommandException("IO error while executing system command", e);
			}
			catch (InterruptedException e) {
				throw new SystemCommandException("Interrupted while executing system command", e);
			}
		}
	}

}
