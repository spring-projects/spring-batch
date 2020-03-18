/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.step;

import java.time.Duration;
import java.util.Date;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.support.ExitCodeMapper;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.metrics.BatchMetrics;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link Step} implementation that provides common behavior to subclasses, including registering and calling
 * listeners.
 *
 * @author Dave Syer
 * @author Ben Hale
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 */
public abstract class AbstractStep implements Step, InitializingBean, BeanNameAware {

	private static final Log logger = LogFactory.getLog(AbstractStep.class);

	private String name;

	private int startLimit = Integer.MAX_VALUE;

	private boolean allowStartIfComplete = false;

	private CompositeStepExecutionListener stepExecutionListener = new CompositeStepExecutionListener();

	private JobRepository jobRepository;

	/**
	 * Default constructor.
	 */
	public AbstractStep() {
		super();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(name != null, "A Step must have a name");
		Assert.state(jobRepository != null, "JobRepository is mandatory");
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Set the name property. Always overrides the default value if this object is a Spring bean.
	 * @param name the name of the {@link Step}.
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of the callbacks in a Spring container the
	 * name property will be set first if it is present. Care is needed with bean definition inheritance - if a parent
	 * bean has a name, then its children need an explicit name as well, otherwise they will not be unique.
	 *
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	@Override
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	@Override
	public int getStartLimit() {
		return this.startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 *
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit == 0 ? Integer.MAX_VALUE : startLimit;
	}

	@Override
	public boolean isAllowStartIfComplete() {
		return this.allowStartIfComplete;
	}

	/**
	 * Public setter for flag that determines whether the step should start again if it is already complete. Defaults to
	 * false.
	 *
	 * @param allowStartIfComplete the value of the flag to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	/**
	 * Convenient constructor for setting only the name property.
	 *
	 * @param name Name of the step
	 */
	public AbstractStep(String name) {
		this.name = name;
	}

	/**
	 * Extension point for subclasses to execute business logic. Subclasses should set the {@link ExitStatus} on the
	 * {@link StepExecution} before returning.
	 *
	 * @param stepExecution the current step context
	 * @throws Exception checked exception thrown by implementation
	 */
	protected abstract void doExecute(StepExecution stepExecution) throws Exception;

	/**
	 * Extension point for subclasses to provide callbacks to their collaborators at the beginning of a step, to open or
	 * acquire resources. Does nothing by default.
	 *
	 * @param ctx the {@link ExecutionContext} to use
	 * @throws Exception checked exception thrown by implementation
	 */
	protected void open(ExecutionContext ctx) throws Exception {
	}

	/**
	 * Extension point for subclasses to provide callbacks to their collaborators at the end of a step (right at the end
	 * of the finally block), to close or release resources. Does nothing by default.
	 *
	 * @param ctx the {@link ExecutionContext} to use
	 * @throws Exception checked exception thrown by implementation
	 */
	protected void close(ExecutionContext ctx) throws Exception {
	}

	/**
	 * Template method for step execution logic - calls abstract methods for resource initialization (
	 * {@link #open(ExecutionContext)}), execution logic ({@link #doExecute(StepExecution)}) and resource closing (
	 * {@link #close(ExecutionContext)}).
	 */
	@Override
	public final void execute(StepExecution stepExecution) throws JobInterruptedException,
	UnexpectedJobExecutionException {

		Assert.notNull(stepExecution, "stepExecution must not be null");

		if (logger.isDebugEnabled()) {
			logger.debug("Executing: id=" + stepExecution.getId());
		}
		stepExecution.setStartTime(new Date());
		stepExecution.setStatus(BatchStatus.STARTED);
		Timer.Sample sample = BatchMetrics.createTimerSample();
		getJobRepository().update(stepExecution);

		// Start with a default value that will be trumped by anything
		ExitStatus exitStatus = ExitStatus.EXECUTING;

		doExecutionRegistration(stepExecution);

		try {
			getCompositeListener().beforeStep(stepExecution);
			open(stepExecution.getExecutionContext());

			try {
				doExecute(stepExecution);
			}
			catch (RepeatException e) {
				throw e.getCause();
			}
			exitStatus = ExitStatus.COMPLETED.and(stepExecution.getExitStatus());

			// Check if someone is trying to stop us
			if (stepExecution.isTerminateOnly()) {
				throw new JobInterruptedException("JobExecution interrupted.");
			}

			// Need to upgrade here not set, in case the execution was stopped
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			if (logger.isDebugEnabled()) {
				logger.debug("Step execution success: id=" + stepExecution.getId());
			}
		}
		catch (Throwable e) {
			stepExecution.upgradeStatus(determineBatchStatus(e));
			exitStatus = exitStatus.and(getDefaultExitStatusForFailure(e));
			stepExecution.addFailureException(e);
			if (stepExecution.getStatus() == BatchStatus.STOPPED) {
				logger.info(String.format("Encountered interruption executing step %s in job %s : %s", name, stepExecution.getJobExecution().getJobInstance().getJobName(), e.getMessage()));
				if (logger.isDebugEnabled()) {
					logger.debug("Full exception", e);
				}
			}
			else {
				logger.error(String.format("Encountered an error executing step %s in job %s", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
			}
		}
		finally {

			try {
				// Update the step execution to the latest known value so the
				// listeners can act on it
				exitStatus = exitStatus.and(stepExecution.getExitStatus());
				stepExecution.setExitStatus(exitStatus);
				exitStatus = exitStatus.and(getCompositeListener().afterStep(stepExecution));
			}
			catch (Exception e) {
				logger.error(String.format("Exception in afterStep callback in step %s in job %s", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
			}

			try {
				getJobRepository().updateExecutionContext(stepExecution);
			}
			catch (Exception e) {
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				exitStatus = exitStatus.and(ExitStatus.UNKNOWN);
				stepExecution.addFailureException(e);
				logger.error(String.format("Encountered an error saving batch meta data for step %s in job %s. "
						+ "This job is now in an unknown state and should not be restarted.", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
			}

			sample.stop(BatchMetrics.createTimer("step", "Step duration",
					Tag.of("job.name", stepExecution.getJobExecution().getJobInstance().getJobName()),
					Tag.of("name", stepExecution.getStepName()),
					Tag.of("status", stepExecution.getExitStatus().getExitCode())
			));
			stepExecution.setEndTime(new Date());
			stepExecution.setExitStatus(exitStatus);
			Duration stepExecutionDuration = BatchMetrics.calculateDuration(stepExecution.getStartTime(), stepExecution.getEndTime());
			logger.info("Step: [" + stepExecution.getStepName() + "] executed in " + BatchMetrics.formatDuration(stepExecutionDuration));

			try {
				getJobRepository().update(stepExecution);
			}
			catch (Exception e) {
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				stepExecution.setExitStatus(exitStatus.and(ExitStatus.UNKNOWN));
				stepExecution.addFailureException(e);
				logger.error(String.format("Encountered an error saving batch meta data for step %s in job %s. "
						+ "This job is now in an unknown state and should not be restarted.", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
			}

			try {
				close(stepExecution.getExecutionContext());
			}
			catch (Exception e) {
				logger.error(String.format("Exception while closing step execution resources in step %s in job %s", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
				stepExecution.addFailureException(e);
			}

			doExecutionRelease();

			if (logger.isDebugEnabled()) {
				logger.debug("Step execution complete: " + stepExecution.getSummary());
			}
		}
	}

	/**
	 * Releases the most recent {@link StepExecution}
	 */
	protected void doExecutionRelease() {
		StepSynchronizationManager.release();
	}

	/**
	 * Registers the {@link StepExecution} for property resolution via {@link StepScope}
	 *
	 * @param stepExecution StepExecution to use when hydrating the StepScoped beans
	 */
	protected void doExecutionRegistration(StepExecution stepExecution) {
		StepSynchronizationManager.register(stepExecution);
	}

	/**
	 * Determine the step status based on the exception.
	 */
	private static BatchStatus determineBatchStatus(Throwable e) {
		if (e instanceof JobInterruptedException || e.getCause() instanceof JobInterruptedException) {
			return BatchStatus.STOPPED;
		}
		else {
			return BatchStatus.FAILED;
		}
	}

	/**
	 * Register a step listener for callbacks at the appropriate stages in a step execution.
	 *
	 * @param listener a {@link StepExecutionListener}
	 */
	public void registerStepExecutionListener(StepExecutionListener listener) {
		this.stepExecutionListener.register(listener);
	}

	/**
	 * Register each of the objects as listeners.
	 *
	 * @param listeners an array of listener objects of known types.
	 */
	public void setStepExecutionListeners(StepExecutionListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			registerStepExecutionListener(listeners[i]);
		}
	}

	/**
	 * @return composite listener that delegates to all registered listeners.
	 */
	protected StepExecutionListener getCompositeListener() {
		return stepExecutionListener;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 *
	 * @param jobRepository is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	protected JobRepository getJobRepository() {
		return jobRepository;
	}

	@Override
	public String toString() {
		return ClassUtils.getShortName(getClass()) + ": [name=" + name + "]";
	}

	/**
	 * Default mapping from throwable to {@link ExitStatus}. Clients can modify the exit code using a
	 * {@link StepExecutionListener}.
	 *
	 * @param ex the cause of the failure
	 * @return an {@link ExitStatus}
	 */
	private ExitStatus getDefaultExitStatusForFailure(Throwable ex) {
		ExitStatus exitStatus;
		if (ex instanceof JobInterruptedException || ex.getCause() instanceof JobInterruptedException) {
			exitStatus = ExitStatus.STOPPED.addExitDescription(JobInterruptedException.class.getName());
		}
		else if (ex instanceof NoSuchJobException || ex.getCause() instanceof NoSuchJobException) {
			exitStatus = new ExitStatus(ExitCodeMapper.NO_SUCH_JOB, ex.getClass().getName());
		}
		else {
			exitStatus = ExitStatus.FAILED.addExitDescription(ex);
		}

		return exitStatus;
	}

}
