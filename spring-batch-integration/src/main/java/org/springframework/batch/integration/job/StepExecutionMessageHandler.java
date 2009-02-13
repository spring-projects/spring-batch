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
package org.springframework.batch.integration.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * @author Dave Syer
 * 
 */
@MessageEndpoint
public class StepExecutionMessageHandler {

	private Step step;

	private JobRepository jobRepository;

	/**
	 * Public setter for the {@link Step}.
	 * @param step the step to set
	 */
	@Required
	public void setStep(Step step) {
		this.step = step;
	}

	/**
	 * Public setter for the {@link JobRepository} that is needed to manage the
	 * state of the batch meta domain (jobs, steps, executions) during the life
	 * of a job.
	 * 
	 * @param jobRepository
	 */
	@Required
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	@ServiceActivator
	public JobExecutionRequest handle(JobExecutionRequest request) {

		// Hand off immediately if the job has already failed
		if (isComplete(request)) {
			return request;
		}

		JobExecution jobExecution = request.getJobExecution();
		JobInstance jobInstance = jobExecution.getJobInstance();

		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());
		try {

			StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, step.getName());

			// Even if it completed successfully we want to pass on the output
			// attributes, so set up the execution context here if it is
			// available.
			if (lastStepExecution != null) {
				stepExecution.setExecutionContext(lastStepExecution.getExecutionContext());
			}

			// If it is already complete and not restartable it will simply be
			// skipped
			if (shouldStart(lastStepExecution, step)) {

				if (!isRestart(jobInstance, lastStepExecution)) {
					stepExecution.setExecutionContext(new ExecutionContext());
				}
				jobRepository.add(stepExecution);
				step.execute(stepExecution);

			}
			else if (lastStepExecution != null) {

				/*
				 * We only set these if the step is not going to execute. They
				 * might be needed by the next step to receive the request, but
				 * they won't be persisted because the step is not executed.
				 */
				stepExecution.setStatus(lastStepExecution.getStatus());
				stepExecution.setExitStatus(lastStepExecution.getExitStatus());

			}

			// (the job might actually not be complete, but the stage is).
			request.setStatus(BatchStatus.COMPLETED);

		}
		catch (Exception e) {
			handleFailure(request, e);
		}
		catch (Error e) {
			handleFailure(request, e);
			throw e;
		}

		return request;

	}

	/**
	 * @param jobInstance
	 * @param lastStepExecution
	 * @return
	 */
	private boolean isRestart(JobInstance jobInstance, StepExecution lastStepExecution) {
		return (lastStepExecution != null && !lastStepExecution.getStatus().equals(BatchStatus.COMPLETED));
	}

	/**
	 * @param request
	 * @return
	 */
	private boolean isComplete(JobExecutionRequest request) {
		return request.getStatus() == BatchStatus.INCOMPLETE || request.getStatus() == BatchStatus.FAILED
				|| request.getStatus() == BatchStatus.STOPPING;
	}

	/**
	 * @param request
	 * @param e
	 */
	private void handleFailure(JobExecutionRequest request, Throwable e) {
		request.registerThrowable(e);
		request.setStatus(BatchStatus.INCOMPLETE);
	}

	/*
	 * TODO: merge this with SimpleJob implementation.
	 * 
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 */
	private boolean shouldStart(StepExecution lastStepExecution, Step step) throws JobExecutionException {

		BatchStatus stepStatus;
		// if the last execution is null, the step has never been executed.
		if (lastStepExecution == null) {
			return true;
		}
		else {
			stepStatus = lastStepExecution.getStatus();
		}

		if (stepStatus == BatchStatus.UNKNOWN) {
			throw new JobExecutionException("Cannot restart step from UNKNOWN status.  "
					+ "The last execution may have ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed.  " + "Manual intervention is probably necessary.");
		}

		if (stepStatus == BatchStatus.COMPLETED && step.isAllowStartIfComplete() == false) {
			// step is complete, false should be returned, indicating that the
			// step should not be started
			return false;
		}

		if (jobRepository.getStepExecutionCount(lastStepExecution.getJobExecution().getJobInstance(), step.getName()) < step
				.getStartLimit()) {
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
