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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.launch.support.ExitCodeMapper;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link Step} implementation that provides common behavior to subclasses,
 * including registering and calling listeners.
 * 
 * @author Dave Syer
 * @author Ben Hale
 * @author Robert Kasanicky
 */
public abstract class AbstractStep implements Step, InitializingBean, BeanNameAware {

	/**
	 * Exit code for interrupted status.
	 */
	public static final String JOB_INTERRUPTED = "JOB_INTERRUPTED";

	private static final Log logger = LogFactory.getLog(AbstractStep.class);

	private String name;

	private int startLimit = Integer.MAX_VALUE;

	private boolean allowStartIfComplete = false;

	private CompositeStepExecutionListener listener = new CompositeStepExecutionListener();

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
	 * Extension point for subclasses to execute business logic. 
	 * 
	 * @param stepExecution
	 * @return {@link ExitStatus} to show whether the step is finished
	 * processing.
	 * @throws Exception
	 */
	protected abstract ExitStatus doExecute(StepExecution stepExecution) throws Exception;

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
	 * block), to close or release resources.  Does nothing by default.
	 * 
	 * @param ctx the {@link ExecutionContext} to use
	 * @throws Exception
	 */
	protected void close(ExecutionContext ctx) throws Exception {
	}

	/**
	 * Template method for step execution logic - calls abstract methods for
	 * resource initialization ({@link #open(ExecutionContext)}), execution
	 * logic ({@link #doExecute(StepExecution)}) and resource closing ({@link #close(ExecutionContext)}).
	 */
	public final void execute(StepExecution stepExecution) throws JobInterruptedException,
			UnexpectedJobExecutionException {
		stepExecution.setStartTime(new Date());
		stepExecution.setStatus(BatchStatus.STARTED);
		getJobRepository().save(stepExecution);

		ExitStatus exitStatus = ExitStatus.FAILED;
		Exception commitException = null;

		try {
			getCompositeListener().beforeStep(stepExecution);
			try {
				open(stepExecution.getExecutionContext());
			}
			catch (Exception e) {
				throw new UnexpectedJobExecutionException("Failed to initialize the step", e);
			}
			exitStatus = doExecute(stepExecution);

			// Check if someone is trying to stop us
			if (stepExecution.isTerminateOnly()) {
				stepExecution.setStatus(BatchStatus.STOPPED);
				throw new JobInterruptedException("JobExecution interrupted.");
			}

			stepExecution.setStatus(BatchStatus.COMPLETED);
			exitStatus = exitStatus.and(getCompositeListener().afterStep(stepExecution));

			try {
				getJobRepository().update(stepExecution);
				getJobRepository().persistExecutionContext(stepExecution);
			}
			catch (Exception e) {
				commitException = e;
				exitStatus = exitStatus.and(ExitStatus.UNKNOWN);
			}

		}
		catch (Throwable e) {

			logger.error("Encountered an error executing the step: " + e.getClass() + ": " + e.getMessage());
			stepExecution.setStatus(determineBatchStatus(e));
			exitStatus = getDefaultExitStatusForFailure(e);

			try {
				exitStatus = exitStatus.and(getCompositeListener().onErrorInStep(stepExecution, e));
				getJobRepository().persistExecutionContext(stepExecution);
			}
			catch (Exception ex) {
				logger.error("Encountered an error on listener error callback.", ex);
			}
			rethrow(e);
		}
		finally {

			stepExecution.setExitStatus(exitStatus);
			stepExecution.setEndTime(new Date());

			try {
				getJobRepository().update(stepExecution);
			}
			catch (Exception e) {
				if (commitException == null) {
					commitException = e;
				}
				else {
					logger.error("Exception while updating step execution after commit exception", e);
				}
			}

			try {
				close(stepExecution.getExecutionContext());
			}
			catch (Exception e) {
				logger.error("Exception while closing step execution resources", e);
				throw new UnexpectedJobExecutionException("Exception while closing step resources", e);
			}

			if (commitException != null) {
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				logger.error("Encountered an error saving batch meta data."
						+ "This job is now in an unknown state and should not be restarted.", commitException);
				throw new UnexpectedJobExecutionException("Encountered an error saving batch meta data.",
						commitException);
			}
		}
	}

	private static void rethrow(Throwable e) throws JobInterruptedException {
		if (e instanceof Error) {
			throw (Error) e;
		}
		if (e instanceof JobInterruptedException) {
			throw (JobInterruptedException) e;
		}
		else if (e.getCause() instanceof JobInterruptedException) {
			throw (JobInterruptedException) e.getCause();
		}
		else if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		}
		throw new UnexpectedJobExecutionException("Unexpected checked exception in step execution", e);
	}

	/**
	 * Determine the step status based on the exception.
	 */
	private static BatchStatus determineBatchStatus(Throwable e) {
		if (e instanceof FatalException) {
			return BatchStatus.UNKNOWN;
		}
		else if (e instanceof JobInterruptedException || e.getCause() instanceof JobInterruptedException) {
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
		this.listener.register(listener);
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
		return listener;
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
			exitStatus = new ExitStatus(false, JOB_INTERRUPTED, JobInterruptedException.class.getName());
		}
		else if (ex instanceof NoSuchJobException || ex.getCause() instanceof NoSuchJobException) {
			exitStatus = new ExitStatus(false, ExitCodeMapper.NO_SUCH_JOB);
		}
		else {
			String message = "";
			StringWriter writer = new StringWriter();
			ex.printStackTrace(new PrintWriter(writer));
			message = writer.toString();
			exitStatus = ExitStatus.FAILED.addExitDescription(message);
		}

		return exitStatus;
	}

	/**
	 * Signals a fatal exception - e.g. unable to persist batch metadata or
	 * rollback transaction. Throwing this exception will result in storing
	 * {@link BatchStatus#UNKNOWN} as step's status.
	 */
	protected static class FatalException extends RuntimeException {
		public FatalException(String string, Throwable e) {
			super(string, e);
		}
	}

}