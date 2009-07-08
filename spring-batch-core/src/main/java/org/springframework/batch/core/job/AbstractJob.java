/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.batch.core.job;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.CompositeJobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract implementation of the {@link Job} interface.  Common dependencies such as a
 * {@link JobRepository}, {@link JobExecutionListener}s, and various configuration 
 * parameters are set here.  Therefore, common error handling and listener calling
 * activities are abstracted away from implementations.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public abstract class AbstractJob implements Job, StepLocator, BeanNameAware, InitializingBean {

	protected static final Log logger = LogFactory.getLog(AbstractJob.class);

	private String name;

	private boolean restartable = true;

	private JobRepository jobRepository;

	private CompositeJobExecutionListener listener = new CompositeJobExecutionListener();

	private JobParametersIncrementer jobParametersIncrementer;

	/**
	 * Default constructor.
	 */
	public AbstractJob() {
		super();
	}

	/**
	 * Convenience constructor to immediately add name (which is mandatory but
	 * not final).
	 * 
	 * @param name
	 */
	public AbstractJob(String name) {
		super();
		this.name = name;
	}

	/**
	 * Assert mandatory properties: {@link JobRepository}.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRepository, "JobRepository must be set");
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

	/**
	 * Set the name property. Always overrides the default value if this object
	 * is a Spring bean.
	 * 
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.domain.IJob#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieve the step with the given name. If there is no Step with the
	 * given name, then return null.
	 * 
	 * @param stepName
	 * @return the Step
	 */
	public abstract Step getStep(String stepName);
	
	/**
	 * Retrieve the step names.
	 * 
	 * @return the step names
	 */
	public abstract Collection<String> getStepNames();
	
	/**
	 * Boolean flag to prevent categorically a job from restarting, even if it
	 * has failed previously.
	 * 
	 * @param restartable the value of the flag to set (default true)
	 */
	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	/**
	 * @see Job#isRestartable()
	 */
	public boolean isRestartable() {
		return restartable;
	}

	/**
	 * Public setter for the {@link JobParametersIncrementer}.
	 * @param jobParametersIncrementer the {@link JobParametersIncrementer} to
	 * set
	 */
	public void setJobParametersIncrementer(JobParametersIncrementer jobParametersIncrementer) {
		this.jobParametersIncrementer = jobParametersIncrementer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.Job#getJobParametersIncrementer()
	 */
	public JobParametersIncrementer getJobParametersIncrementer() {
		return this.jobParametersIncrementer;
	}

	/**
	 * Public setter for injecting {@link JobExecutionListener}s. They will all
	 * be given the listener callbacks at the appropriate point in the job.
	 * 
	 * @param listeners the listeners to set.
	 */
	public void setJobExecutionListeners(JobExecutionListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			this.listener.register(listeners[i]);
		}
	}

	/**
	 * Register a single listener for the {@link JobExecutionListener}
	 * callbacks.
	 * 
	 * @param listener a {@link JobExecutionListener}
	 */
	public void registerJobExecutionListener(JobExecutionListener listener) {
		this.listener.register(listener);
	}

	/**
	 * Public setter for the {@link JobRepository} that is needed to manage the
	 * state of the batch meta domain (jobs, steps, executions) during the life
	 * of a job.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Extension point for subclasses allowing them to concentrate on processing
	 * logic and ignore listeners and repository calls. Implementations usually
	 * are concerned with the ordering of steps, and delegate actual step
	 * processing to {@link #handleStep(Step, JobExecution)}.
	 * 
	 * @param execution the current {@link JobExecution}
	 * 
	 * @throws JobExecutionException to signal a fatal batch framework error
	 * (not a business or validation exception)
	 */
	abstract protected void doExecute(JobExecution execution) throws JobExecutionException;

	/**
	 * Run the specified job, handling all listener and repository calls, and
	 * delegating the actual processing to {@link #doExecute(JobExecution)}.
	 * 
	 * @see Job#execute(JobExecution)
	 * @throws StartLimitExceededException if start limit of one of the steps
	 * was exceeded
	 */
	public final void execute(JobExecution execution) {

		logger.debug("Job execution starting: "+execution);					

		try {

			if (execution.getStatus() != BatchStatus.STOPPING) {

				execution.setStartTime(new Date());
				updateStatus(execution, BatchStatus.STARTED);

				listener.beforeJob(execution);

				try {
					doExecute(execution);
					logger.debug("Job execution complete: "+execution);					
				} catch (RepeatException e) {
					throw e.getCause();
				}
			}
			else {

				// The job was already stopped before we even got this far. Deal
				// with it in the same way as any other interruption.
				execution.setStatus(BatchStatus.STOPPED);
				execution.setExitStatus(ExitStatus.COMPLETED);
				logger.debug("Job execution was stopped: "+execution);					

			}

		}
		catch (JobInterruptedException e) {
			logger.error("Encountered interruption executing job", e);
			execution.setExitStatus(ExitStatus.STOPPED);
			execution.setStatus(BatchStatus.STOPPED);
			execution.addFailureException(e);
		}
		catch (Throwable t) {
			logger.error("Encountered fatal error executing job", t);
			execution.setExitStatus(ExitStatus.FAILED);
			execution.setStatus(BatchStatus.FAILED);
			execution.addFailureException(t);
		}
		finally {

			if (execution.getStepExecutions().isEmpty()) {
				execution.setExitStatus(ExitStatus.NOOP
						.addExitDescription("All steps already completed or no steps configured for this job."));
			}

			execution.setEndTime(new Date());

			try {
				listener.afterJob(execution);
			}
			catch (Exception e) {
				logger.error("Exception encountered in afterStep callback", e);
			}
			
			jobRepository.update(execution);
			
		}

	}

	/**
	 * Convenience method for subclasses to delegate the handling of a specific
	 * step in the context of the current {@link JobExecution}. Clients of this
	 * method do not need access to the {@link JobRepository}, nor do they need
	 * to worry about populating the execution context on a restart, nor
	 * detecting the interrupted state (in job or step execution).
	 * 
	 * @param step the {@link Step} to execute
	 * @param execution the current {@link JobExecution}
	 * @return the {@link StepExecution} corresponding to this step
	 * 
	 * @throws JobInterruptedException if the {@link JobExecution} has been
	 * interrupted, and in particular if {@link BatchStatus#ABANDONED} or
	 * {@link BatchStatus#STOPPING} is detected
	 * @throws StartLimitExceededException if the start limit has been exceeded
	 * for this step
	 * @throws JobRestartException if the job is in an inconsistent state from
	 * an earlier failure
	 */
	protected final StepExecution handleStep(Step step, JobExecution execution) throws JobInterruptedException,
			JobRestartException, StartLimitExceededException {
		if (execution.isStopping()) {
			throw new JobInterruptedException("JobExecution interrupted.");
		}

		JobInstance jobInstance = execution.getJobInstance();

		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, step.getName());
		StepExecution currentStepExecution = lastStepExecution;

		if (shouldStart(lastStepExecution, jobInstance, step)) {

			currentStepExecution = execution.createStepExecution(step.getName());

			boolean isRestart = (lastStepExecution != null && !lastStepExecution.getStatus().equals(
					BatchStatus.COMPLETED));

			if (isRestart) {
				currentStepExecution.setExecutionContext(lastStepExecution.getExecutionContext());
			}
			else {
				currentStepExecution.setExecutionContext(new ExecutionContext());
			}

			jobRepository.add(currentStepExecution);

			logger.info("Executing step: [" + step + "]");
			step.execute(currentStepExecution);

			jobRepository.updateExecutionContext(execution);

			if (currentStepExecution.getStatus() == BatchStatus.STOPPING || currentStepExecution.getStatus() == BatchStatus.STOPPED) {
				throw new JobInterruptedException("Job interrupted by step execution");
			}

		} else {
			// currentStepExecution.setExitStatus(ExitStatus.NOOP);
		}

		return currentStepExecution;

	}

	/**
	 * Convenience method for subclasses so they can change the state of a
	 * {@link StepExecution} if necessary. Use with care (and not at all
	 * preferably) and only before or after a step is executed.
	 * 
	 * @param stepExecution
	 */
	protected void updateStepExecution(StepExecution stepExecution) {
		jobRepository.update(stepExecution);
	}

	/**
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 * @param lastStepExecution the last step execution
	 * @param jobInstance
	 * @param step
	 * 
	 * @throws StartLimitExceededException if the start limit has been exceeded
	 * for this step
	 * @throws JobRestartException if the job is in an inconsistent state from
	 * an earlier failure
	 */
	private boolean shouldStart(StepExecution lastStepExecution, JobInstance jobInstance, Step step)
			throws JobRestartException, StartLimitExceededException {

		BatchStatus stepStatus;
		if (lastStepExecution == null) {
			stepStatus = BatchStatus.STARTING;
		}
		else {
			stepStatus = lastStepExecution.getStatus();
		}

		if (stepStatus == BatchStatus.UNKNOWN) {
			throw new JobRestartException("Cannot restart step from UNKNOWN status.  "
					+ "The last execution ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed.  " + "Manual intervention is probably necessary.");
		}

		if ((stepStatus == BatchStatus.COMPLETED && step.isAllowStartIfComplete() == false) 
				|| stepStatus == BatchStatus.ABANDONED) {
			// step is complete, false should be returned, indicating that the
			// step should not be started
			logger.info("Step already complete or not restartable, so no action to execute: "+lastStepExecution);
			return false;
		}

		if (jobRepository.getStepExecutionCount(jobInstance, step.getName()) < step.getStartLimit()) {
			// step start count is less than start max, return true
			return true;
		}
		else {
			// start max has been exceeded, throw an exception.
			throw new StartLimitExceededException("Maximum start limit exceeded for step: " + step.getName()
					+ "StartMax: " + step.getStartLimit());
		}
	}

	private void updateStatus(JobExecution jobExecution, BatchStatus status) {
		jobExecution.setStatus(status);
		jobRepository.update(jobExecution);
	}

	public String toString() {
		return ClassUtils.getShortName(getClass()) + ": [name=" + name + "]";
	}

}
