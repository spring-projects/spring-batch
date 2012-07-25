/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.step;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.support.ExitCodeMapper;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link Step} implementation that provides common behavior to subclasses,
 * including registering and calling listeners.
 * 
 * @author Dave Syer
 * @author Ben Hale
 * @author Robert Kasanicky
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

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRepository, "JobRepository is mandatory");
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Set the name property. Always overrides the default value if this object
	 * is a Spring bean.
	 * 
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of
	 * the callbacks in a Spring container the name property will be set first
	 * if it is present. Care is needed with bean definition inheritance - if a
	 * parent bean has a name, then its children need an explicit name as well,
	 * otherwise they will not be unique.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	public int getStartLimit() {
		return this.startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 * 
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	public boolean isAllowStartIfComplete() {
		return this.allowStartIfComplete;
	}

	/**
	 * Public setter for flag that determines whether the step should start
	 * again if it is already complete. Defaults to false.
	 * 
	 * @param allowStartIfComplete the value of the flag to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	/**
	 * Convenient constructor for setting only the name property.
	 * 
	 * @param name
	 */
	public AbstractStep(String name) {
		this.name = name;
	}

	/**
	 * Extension point for subclasses to execute business logic. Subclasses
	 * should set the {@link ExitStatus} on the {@link StepExecution} before
	 * returning.
	 * 
	 * @param stepExecution the current step context
	 * @throws Exception
	 */
	protected abstract void doExecute(StepExecution stepExecution) throws Exception;

	/**
	 * Extension point for subclasses to provide callbacks to their
	 * collaborators at the beginning of a step, to open or acquire resources.
	 * Does nothing by default.
	 * 
	 * @param ctx the {@link ExecutionContext} to use
	 * @throws Exception
	 */
	protected void open(ExecutionContext ctx) throws Exception {
	}

	/**
	 * Extension point for subclasses to provide callbacks to their
	 * collaborators at the end of a step (right at the end of the finally
	 * block), to close or release resources. Does nothing by default.
	 * 
	 * @param ctx the {@link ExecutionContext} to use
	 * @throws Exception
	 */
	protected void close(ExecutionContext ctx) throws Exception {
	}

	/**
	 * Template method for step execution logic - calls abstract methods for
	 * resource initialization ({@link #open(ExecutionContext)}), execution
	 * logic ({@link #doExecute(StepExecution)}) and resource closing (
	 * {@link #close(ExecutionContext)}).
	 */
	public final void execute(StepExecution stepExecution) throws JobInterruptedException,
			UnexpectedJobExecutionException {

		logger.debug("Executing: id=" + stepExecution.getId());
		stepExecution.setStartTime(new Date());
		stepExecution.setStatus(BatchStatus.STARTED);
		getJobRepository().update(stepExecution);

		// Start with a default value that will be trumped by anything
		ExitStatus exitStatus = ExitStatus.EXECUTING;

		StepSynchronizationManager.register(stepExecution);

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
			logger.debug("Step execution success: id=" + stepExecution.getId());
		}
		catch (Throwable e) {
			stepExecution.upgradeStatus(determineBatchStatus(e));
			exitStatus = exitStatus.and(getDefaultExitStatusForFailure(e));
			stepExecution.addFailureException(e);
			if (stepExecution.getStatus() == BatchStatus.STOPPED) {
				logger.info("Encountered interruption executing step: " + e.getMessage());
				if (logger.isDebugEnabled()) {
					logger.debug("Full exception", e);
				}
			}
			else {
				logger.error("Encountered an error executing the step", e);
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
				logger.error("Exception in afterStep callback", e);
			}

			try {
				getJobRepository().updateExecutionContext(stepExecution);
			}
			catch (Exception e) {
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				exitStatus = exitStatus.and(ExitStatus.UNKNOWN);
				stepExecution.addFailureException(e);
				logger.error("Encountered an error saving batch meta data. "
						+ "This job is now in an unknown state and should not be restarted.", e);
			}

			stepExecution.setEndTime(new Date());
			stepExecution.setExitStatus(exitStatus);

			try {
				getJobRepository().update(stepExecution);
			}
			catch (Exception e) {
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				stepExecution.setExitStatus(exitStatus.and(ExitStatus.UNKNOWN));
				stepExecution.addFailureException(e);
				logger.error("Encountered an error saving batch meta data. "
						+ "This job is now in an unknown state and should not be restarted.", e);
			}

			try {
				close(stepExecution.getExecutionContext());
			}
			catch (Exception e) {
				logger.error("Exception while closing step execution resources", e);
				stepExecution.addFailureException(e);
			}

			StepSynchronizationManager.release();

			logger.debug("Step execution complete: " + stepExecution.getSummary());
		}
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
	 * Register a step listener for callbacks at the appropriate stages in a
	 * step execution.
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

	public String toString() {
		return ClassUtils.getShortName(getClass()) + ": [name=" + name + "]";
	}

	/**
	 * Default mapping from throwable to {@link ExitStatus}. Clients can modify
	 * the exit code using a {@link StepExecutionListener}.
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