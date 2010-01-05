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
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.StepHandler;
import org.springframework.batch.core.repository.JobRepository;
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

	private StepHandler stepHandler;

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
		stepHandler = new SimpleStepHandler(jobRepository);
	}

	@ServiceActivator
	public JobExecutionRequest handle(JobExecutionRequest request) {

		// Hand off immediately if the job has already failed
		if (isComplete(request)) {
			return request;
		}

		JobExecution jobExecution = request.getJobExecution();

		try {
			
			stepHandler.handleStep(step, jobExecution);
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
	 * @param request
	 * @return
	 */
	private boolean isComplete(JobExecutionRequest request) {
		return request.getStatus() == BatchStatus.FAILED || request.getStatus() == BatchStatus.ABANDONED
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

}
