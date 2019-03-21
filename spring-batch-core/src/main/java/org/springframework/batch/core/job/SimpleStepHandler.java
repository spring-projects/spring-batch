/*
 * Copyright 2006-2014 the original author or authors.
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

package org.springframework.batch.core.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Implementation of {@link StepHandler} that manages repository and restart
 * concerns.
 *
 * @author Dave Syer
 *
 */
public class SimpleStepHandler implements StepHandler, InitializingBean {

	private static final Log logger = LogFactory.getLog(SimpleStepHandler.class);

	private JobRepository jobRepository;

	private ExecutionContext executionContext;

	/**
	 * Convenient default constructor for configuration usage.
	 */
	public SimpleStepHandler() {
		this(null);
	}

	/**
	 * @param jobRepository a {@link org.springframework.batch.core.repository.JobRepository}
	 */
	public SimpleStepHandler(JobRepository jobRepository) {
		this(jobRepository, new ExecutionContext());
	}

	/**
	 * @param jobRepository a {@link org.springframework.batch.core.repository.JobRepository}
	 * @param executionContext the {@link org.springframework.batch.item.ExecutionContext} for the current Step
	 */
	public SimpleStepHandler(JobRepository jobRepository, ExecutionContext executionContext) {
		this.jobRepository = jobRepository;
		this.executionContext = executionContext;
	}

	/**
	 * Check mandatory properties (jobRepository).
	 *
	 * @see InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jobRepository != null, "A JobRepository must be provided");
	}

	/**
	 * @return the used jobRepository
	 */
	protected JobRepository getJobRepository() {
		return this.jobRepository;
	}

	/**
	 * @param jobRepository the jobRepository to set
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * A context containing values to be added to the step execution before it
	 * is handled.
	 *
	 * @param executionContext the execution context to set
	 */
	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	@Override
	public StepExecution handleStep(Step step, JobExecution execution) throws JobInterruptedException,
	JobRestartException, StartLimitExceededException {
		if (execution.isStopping()) {
			throw new JobInterruptedException("JobExecution interrupted.");
		}

		JobInstance jobInstance = execution.getJobInstance();

		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, step.getName());
		if (stepExecutionPartOfExistingJobExecution(execution, lastStepExecution)) {
			// If the last execution of this step was in the same job, it's
			// probably intentional so we want to run it again...
			logger.info(String.format("Duplicate step [%s] detected in execution of job=[%s]. "
					+ "If either step fails, both will be executed again on restart.", step.getName(), jobInstance
					.getJobName()));
			lastStepExecution = null;
		}
		StepExecution currentStepExecution = lastStepExecution;

		if (shouldStart(lastStepExecution, execution, step)) {

			currentStepExecution = execution.createStepExecution(step.getName());

			boolean isRestart = (lastStepExecution != null && !lastStepExecution.getStatus().equals(
					BatchStatus.COMPLETED));

			if (isRestart) {
				currentStepExecution.setExecutionContext(lastStepExecution.getExecutionContext());

				if(lastStepExecution.getExecutionContext().containsKey("batch.executed")) {
					currentStepExecution.getExecutionContext().remove("batch.executed");
				}
			}
			else {
				currentStepExecution.setExecutionContext(new ExecutionContext(executionContext));
			}

			jobRepository.add(currentStepExecution);

			logger.info("Executing step: [" + step.getName() + "]");
			try {
				step.execute(currentStepExecution);
				currentStepExecution.getExecutionContext().put("batch.executed", true);
			}
			catch (JobInterruptedException e) {
				// Ensure that the job gets the message that it is stopping
				// and can pass it on to other steps that are executing
				// concurrently.
				execution.setStatus(BatchStatus.STOPPING);
				throw e;
			}

			jobRepository.updateExecutionContext(execution);

			if (currentStepExecution.getStatus() == BatchStatus.STOPPING
					|| currentStepExecution.getStatus() == BatchStatus.STOPPED) {
				// Ensure that the job gets the message that it is stopping
				execution.setStatus(BatchStatus.STOPPING);
				throw new JobInterruptedException("Job interrupted by step execution");
			}

		}

		return currentStepExecution;
	}

	/**
	 * Detect whether a step execution belongs to this job execution.
	 * @param jobExecution the current job execution
	 * @param stepExecution an existing step execution
	 * @return true if the {@link org.springframework.batch.core.StepExecution} is part of the {@link org.springframework.batch.core.JobExecution}
	 */
	private boolean stepExecutionPartOfExistingJobExecution(JobExecution jobExecution, StepExecution stepExecution) {
		return stepExecution != null && stepExecution.getJobExecutionId() != null
				&& stepExecution.getJobExecutionId().equals(jobExecution.getId());
	}

	/**
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 * @param lastStepExecution the last step execution
	 * @param jobExecution the {@link JobExecution} instance to be evaluated.
	 * @param step the {@link Step} instance to be evaluated.
	 * @return true if step should start, false if it should not.
	 *
	 * @throws StartLimitExceededException if the start limit has been exceeded
	 * for this step
	 * @throws JobRestartException if the job is in an inconsistent state from
	 * an earlier failure
	 */
	protected boolean shouldStart(StepExecution lastStepExecution, JobExecution jobExecution, Step step)
			throws JobRestartException, StartLimitExceededException {

		BatchStatus stepStatus;
		if (lastStepExecution == null) {
			stepStatus = BatchStatus.STARTING;
		}
		else {
			stepStatus = lastStepExecution.getStatus();
		}

		if (stepStatus == BatchStatus.UNKNOWN) {
			throw new JobRestartException("Cannot restart step from UNKNOWN status. "
					+ "The last execution ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed. Manual intervention is probably necessary.");
		}

		if ((stepStatus == BatchStatus.COMPLETED && !step.isAllowStartIfComplete())
				|| stepStatus == BatchStatus.ABANDONED) {
			// step is complete, false should be returned, indicating that the
			// step should not be started
			logger.info("Step already complete or not restartable, so no action to execute: " + lastStepExecution);
			return false;
		}

		if (jobRepository.getStepExecutionCount(jobExecution.getJobInstance(), step.getName()) < step.getStartLimit()) {
			// step start count is less than start max, return true
			return true;
		}
		else {
			// start max has been exceeded, throw an exception.
			throw new StartLimitExceededException("Maximum start limit exceeded for step: " + step.getName()
					+ "StartMax: " + step.getStartLimit());
		}
	}

}
