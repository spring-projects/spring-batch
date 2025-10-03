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

package org.springframework.batch.core.step.tasklet;

import java.io.File;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Tasklet} that executes a system command.
 * <p>
 * The system command is executed asynchronously using injected
 * {@link #setTaskExecutor(TaskExecutor)} - timeout value is required to be set, so that
 * the batch job does not hang forever if the external process hangs.
 * <p>
 * Tasklet periodically checks for termination status (i.e. {@link #setCommand(String...)}
 * finished its execution or {@link #setTimeout(long)} expired or job was interrupted).
 * The check interval is given by {@link #setTerminationCheckInterval(long)}.
 * <p>
 * When job interrupt is detected tasklet's execution is terminated immediately by
 * throwing {@link JobInterruptedException}.
 * <p>
 * {@link #setInterruptOnCancel(boolean)} specifies whether the tasklet should attempt to
 * interrupt the thread that executes the system command if it is still running when
 * tasklet exits (abnormally).
 *
 * @author Robert Kasanicky
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @author Injae Kim
 * @author Hyunsang Han
 */
@NullUnmarked // FIXME to remove once default constructors are removed
public class SystemCommandTasklet implements StepExecutionListener, StoppableTasklet, InitializingBean {

	protected static final Log logger = LogFactory.getLog(SystemCommandTasklet.class);

	private CommandRunner commandRunner = new JvmCommandRunner();

	private String[] cmdArray;

	private String[] environmentParams = null;

	private File workingDirectory = null;

	private SystemProcessExitCodeMapper systemProcessExitCodeMapper = new SimpleSystemProcessExitCodeMapper();

	private long timeout = 0;

	private long checkInterval = 1000;

	private StepExecution execution = null;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private boolean interruptOnCancel = false;

	private volatile boolean stopped = false;

	private JobRepository jobRepository;

	private boolean stoppable = false;

	/**
	 * Execute system command and map its exit code to {@link ExitStatus} using
	 * {@link SystemProcessExitCodeMapper}.
	 */
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		FutureTask<Integer> systemCommandTask = new FutureTask<>(() -> {
			Process process = commandRunner.exec(cmdArray, environmentParams, workingDirectory);
			return process.waitFor();
		});

		long t0 = System.currentTimeMillis();

		taskExecutor.execute(systemCommandTask);

		while (true) {
			Thread.sleep(checkInterval);// moved to the end of the logic

			if (stoppable) {
				JobExecution jobExecution = jobRepository
					.getJobExecution(chunkContext.getStepContext().getStepExecution().getJobExecutionId());

				if (jobExecution.isStopping()) {
					stopped = true;
				}
			}

			if (systemCommandTask.isDone()) {
				Integer exitCode = systemCommandTask.get();
				ExitStatus exitStatus = systemProcessExitCodeMapper.getExitStatus(exitCode);
				contribution.setExitStatus(exitStatus);
				if (ExitStatus.FAILED.equals(exitStatus)) {
					throw new SystemCommandException("Execution of system command failed with exit code " + exitCode);
				}
				else {
					return RepeatStatus.FINISHED;
				}
			}
			else if (System.currentTimeMillis() - t0 > timeout) {
				systemCommandTask.cancel(interruptOnCancel);
				throw new SystemCommandException("Execution of system command did not finish within the timeout");
			}
			else if (execution.isTerminateOnly()) {
				systemCommandTask.cancel(interruptOnCancel);
				String command = String.join(" ", cmdArray);
				throw new JobInterruptedException("Job interrupted while executing system command '" + command + "'");
			}
			else if (stopped) {
				systemCommandTask.cancel(interruptOnCancel);
				contribution.setExitStatus(ExitStatus.STOPPED);
				return RepeatStatus.FINISHED;
			}
		}
	}

	/**
	 * Injection setter for the {@link CommandRunner}.
	 * @param commandRunner {@link CommandRunner} instance to be used by
	 * SystemCommandTasklet instance. Defaults to {@link JvmCommandRunner}.
	 * @since 5.0
	 */
	public void setCommandRunner(CommandRunner commandRunner) {
		this.commandRunner = commandRunner;
	}

	/**
	 * Set the command to execute along with its arguments. For example:
	 *
	 * <pre>setCommand("myCommand", "myArg1", "myArg2");</pre>
	 * @param command command to be executed in a separate system process.
	 */
	public void setCommand(String... command) {
		this.cmdArray = command;
	}

	/**
	 * @param envp environment parameter values, inherited from parent process when not
	 * set (or set to null).
	 */
	public void setEnvironmentParams(String[] envp) {
		this.environmentParams = envp;
	}

	/**
	 * @param dir working directory of the spawned process, inherited from parent process
	 * when not set (or set to null).
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

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(commandRunner != null, "CommandRunner must be set");
		Assert.state(cmdArray != null, "'cmdArray' property value must not be null");
		Assert.state(!ObjectUtils.isEmpty(cmdArray), "'cmdArray' property value is required with at least 1 element");
		Assert.state(StringUtils.hasText(cmdArray[0]), "'cmdArray' property value is required with at least 1 element");
		Assert.state(systemProcessExitCodeMapper != null, "SystemProcessExitCodeMapper must be set");
		Assert.state(timeout > 0, "timeout value must be greater than zero");
		Assert.state(taskExecutor != null, "taskExecutor is required");
		stoppable = jobRepository != null;
	}

	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
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
	 * Timeout in milliseconds.
	 * @param timeout upper limit for how long the execution of the external program is
	 * allowed to last.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * The time interval how often the tasklet will check for termination status.
	 * @param checkInterval time interval in milliseconds (1 second by default).
	 */
	public void setTerminationCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}

	/**
	 * Get a reference to {@link StepExecution} for interrupt checks during system command
	 * execution.
	 */
	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.execution = stepExecution;
	}

	/**
	 * Sets the task executor that will be used to execute the system command NB! Avoid
	 * using a synchronous task executor
	 * @param taskExecutor instance of {@link TaskExecutor}.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * If <code>true</code> tasklet will attempt to interrupt the thread executing the
	 * system command if {@link #setTimeout(long)} has been exceeded or user interrupts
	 * the job. <code>false</code> by default
	 * @param interruptOnCancel boolean determines if process should be interrupted
	 */
	public void setInterruptOnCancel(boolean interruptOnCancel) {
		this.interruptOnCancel = interruptOnCancel;
	}

	/**
	 * Will interrupt the thread executing the system command only if
	 * {@link #setInterruptOnCancel(boolean)} has been set to true. Otherwise the
	 * underlying command will be allowed to finish before the tasklet ends.
	 *
	 * @since 3.0
	 * @see StoppableTasklet#stop()
	 */
	@Override
	public void stop() {
		stopped = true;
	}

	/**
	 * Interrupts the execution of the system command if the given {@link StepExecution}
	 * matches the current execution context. This method allows for granular control over
	 * stopping specific step executions, ensuring that only the intended command is
	 * halted.
	 * <p>
	 * This method will interrupt the thread executing the system command only if
	 * {@link #setInterruptOnCancel(boolean)} has been set to true. Otherwise, the
	 * underlying command will be allowed to finish before the tasklet ends.
	 * @param stepExecution the current {@link StepExecution} context; the execution is
	 * interrupted if it matches the ongoing one.
	 * @since 6.0
	 * @see StoppableTasklet#stop(StepExecution)
	 */
	@Override
	public void stop(StepExecution stepExecution) {
		if (stepExecution.equals(this.execution)) {
			this.stopped = true;
		}
	}

}
