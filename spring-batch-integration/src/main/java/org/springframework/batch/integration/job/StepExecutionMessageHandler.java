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
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.annotation.Handler;

/**
 * @author Dave Syer
 * 
 */
public class StepExecutionMessageHandler {

	private Step step;

	private JobRepository jobRepository;

	private String[] inputKeys = new String[0];

	private String[] outputKeys = new String[0];

	/**
	 * Public setter for the {@link Step}.
	 * @param step the step to set
	 */
	@Required
	public void setStep(Step step) {
		this.step = step;
	}

	/**
	 * Public setter for the input keys. Attributes from the incoming request
	 * will be added to the execution context for the step.
	 * @param inputKeys the inputKeys to set
	 */
	public void setInputKeys(String[] inputKeys) {
		this.inputKeys = inputKeys;
	}

	/**
	 * Public setter for the output keys. Attributes from a successful step
	 * execution context will be added to the request before it is handed on to
	 * the next handler.
	 * @param outputKeys the outputKeys to set
	 */
	public void setOutputKeys(String[] outputKeys) {
		this.outputKeys = outputKeys;
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

	@Handler
	public JobExecutionRequest handle(JobExecutionRequest request) {


		// Hand off immediately if the job has already failed
		if (isComplete(request)) {
			return request;
		}

		JobExecution jobExecution = request.getJobExecution();
		JobInstance jobInstance = jobExecution.getJobInstance();

		StepExecution stepExecution = jobExecution.createStepExecution(step);
		try {

			StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, step);

			// Even if it completed successfully we want to pass on the output
			// attributes, so set up the execution context here if it is
			// available.
			if (lastStepExecution != null) {
				stepExecution.setExecutionContext(lastStepExecution.getExecutionContext());
			}

			// If it is already complete and not restartable it will simply be
			// skipped
			if (shouldStart(lastStepExecution, step)) {

				boolean isRestart = (jobRepository.getStepExecutionCount(jobInstance, step) > 0 && !lastStepExecution
						.getExitStatus().equals(ExitStatus.FINISHED)) ? true : false;

				if (!isRestart || lastStepExecution == null) {
					stepExecution.setExecutionContext(getExecutionContextWithInputs(request));
				}

				step.execute(stepExecution);

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

		// TODO: could a failure here could cause job to be in inconsistent
		// state?
		getMessageWithOutputs(request, stepExecution.getExecutionContext());
		return request;

	}

	/**
	 * Clear the message header of the input attributes and add in output
	 * attributes from the execution context.
	 * @param request
	 * @param executionContext
	 * @return
	 */
	private JobExecutionRequest getMessageWithOutputs(JobExecutionRequest request, ExecutionContext executionContext) {
		for (int i = 0; i < inputKeys.length; i++) {
			String key = inputKeys[i];
			request.removeAttribute(key);
		}
		for (int i = 0; i < outputKeys.length; i++) {
			String key = outputKeys[i];
			request.setAttribute(key, executionContext.get(key));
		}
		// TODO: is this safe, or should we build a new message?
		return request;
	}

	/**
	 * Generate a new execution context with all the attributes requested from
	 * the message (if they exist).
	 * 
	 * @return an {@link ExecutionContext}
	 */
	private ExecutionContext getExecutionContextWithInputs(JobExecutionRequest request) {
		ExecutionContext executionContext = new ExecutionContext();
		for (int i = 0; i < inputKeys.length; i++) {
			String key = inputKeys[i];
			Object value = request.getAttribute(key);
			executionContext.put(key, value);
		}
		return executionContext;
	}

	/**
	 * @param request
	 * @return
	 */
	private boolean isComplete(JobExecutionRequest request) {
		return request.getStatus() == BatchStatus.FAILED || request.getStatus() == BatchStatus.STOPPED
				|| request.getStatus() == BatchStatus.STOPPING;
	}

	/**
	 * @param request
	 * @param e
	 */
	private void handleFailure(JobExecutionRequest request, Throwable e) {
		request.registerThrowable(e);
		request.setStatus(BatchStatus.FAILED);
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
					+ "The last execution ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed.  " + "Manual intervention is probably necessary.");
		}

		if (stepStatus == BatchStatus.COMPLETED && step.isAllowStartIfComplete() == false) {
			// step is complete, false should be returned, indicating that the
			// step should not be started
			return false;
		}

		if (jobRepository.getStepExecutionCount(lastStepExecution.getJobExecution().getJobInstance(), step) < step
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
