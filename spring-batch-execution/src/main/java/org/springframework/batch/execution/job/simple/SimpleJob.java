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

package org.springframework.batch.execution.job.simple;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.common.ExceptionClassifier;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.domain.StepInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.ExitCodeExceptionClassifier;
import org.springframework.batch.execution.step.simple.SimpleExitCodeExceptionClassifier;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simple implementation of (@link Job} interface providing the ability to run a
 * {@link JobExecution}. Sequentially executes a job by iterating it's life of
 * steps.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleJob extends JobSupport {

	private JobRepository jobRepository;

	private ExitCodeExceptionClassifier exceptionClassifier = new SimpleExitCodeExceptionClassifier();

	/**
	 * Run the specified job by looping through the steps and delegating to the
	 * {@link Step}.
	 * 
	 * @see org.springframework.batch.core.domain.Job#run(org.springframework.batch.core.domain.JobExecution)
	 */
	public ExitStatus run(JobExecution execution) throws BatchCriticalException {

		JobInstance jobInstance = execution.getJobInstance();
		updateStatus(execution, BatchStatus.STARTING);

		List stepInstances = jobInstance.getStepInstances();

		ExitStatus status = ExitStatus.FAILED;

		try {

			int startedCount = 0;

			List steps = getSteps();
			for (Iterator i = stepInstances.iterator(), j = steps.iterator(); i.hasNext() && j.hasNext();) {

				StepInstance stepInstance = (StepInstance) i.next();
				Step step = (Step) j.next();

				if (shouldStart(stepInstance, step)) {
					startedCount++;
					updateStatus(execution, BatchStatus.STARTED);
					StepExecution stepExecution = execution.createStepExecution(stepInstance);
					status = step.process(stepExecution);
				}
			}

			if (startedCount == 0) {
				if (steps.size() > 0) {
					status = ExitStatus.NOOP
							.addExitDescription("All steps already completed.  No processing was done.");
				}
				else {
					status = ExitStatus.NOOP.addExitDescription("No steps configured for this job.");
				}
			}

			updateStatus(execution, BatchStatus.COMPLETED);

		}
		catch (StepInterruptedException e) {
			updateStatus(execution, BatchStatus.STOPPED);
			status = exceptionClassifier.classifyForExitCode(e);
			rethrow(e);
		}
		catch (Throwable t) {
			updateStatus(execution, BatchStatus.FAILED);
			status = exceptionClassifier.classifyForExitCode(t);
			rethrow(t);
		}
		finally {
			execution.setEndTime(new Date(System.currentTimeMillis()));
			execution.setExitStatus(status);
			jobRepository.saveOrUpdate(execution);
		}

		return status;
	}

	private void updateStatus(JobExecution jobExecution, BatchStatus status) {
		JobInstance job = jobExecution.getJobInstance();
		jobExecution.setStatus(status);
		job.setStatus(status);
		jobRepository.update(job);
		jobRepository.saveOrUpdate(jobExecution);
	}

	/*
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 */
	private boolean shouldStart(StepInstance stepInstance, Step step) {

		if (stepInstance.getStatus() == BatchStatus.COMPLETED && step.isAllowStartIfComplete() == false) {
			// step is complete, false should be returned, indicated that the
			// step should
			// not be started
			return false;
		}

		if (stepInstance.getStepExecutionCount() < step.getStartLimit()) {
			// step start count is less than start max, return true
			return true;
		}
		else {
			// start max has been exceeded, throw an exception.
			throw new BatchCriticalException("Maximum start limit exceeded for step: " + stepInstance.getName()
					+ "StartMax: " + step.getStartLimit());
		}
	}

	/**
	 * @param t
	 */
	private static void rethrow(Throwable t) throws RuntimeException {
		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		else {
			throw new BatchCriticalException(t);
		}
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
	 * Public setter for injecting an {@link ExceptionClassifier} that can
	 * translate exceptions to {@link ExitStatus}.
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(ExitCodeExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}
}
