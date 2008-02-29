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

package org.springframework.batch.execution.job;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.common.ExceptionClassifier;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.JobListener;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.ExitStatusExceptionClassifier;
import org.springframework.batch.execution.listener.CompositeJobListener;
import org.springframework.batch.execution.scope.SimpleStepContext;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.execution.step.support.SimpleExitStatusExceptionClassifier;
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
public class SimpleJob extends AbstractJob {

	private JobRepository jobRepository;

	private ExitStatusExceptionClassifier exceptionClassifier = new SimpleExitStatusExceptionClassifier();

	private CompositeJobListener listener = new CompositeJobListener();

	public void setListeners(JobListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			this.listener.register(listeners[i]);
		}
	}

	public void setListener(JobListener listener) {
		this.listener.register(listener);
	}

	/**
	 * Run the specified job by looping through the steps and delegating to the
	 * {@link Step}.
	 * 
	 * @see org.springframework.batch.core.domain.Job#execute(org.springframework.batch.core.domain.JobExecution)
	 */
	public void execute(JobExecution execution) throws BatchCriticalException {

		JobInstance jobInstance = execution.getJobInstance();
		jobInstance.setLastExecution(execution);

		ExitStatus status = ExitStatus.FAILED;

		try {

			// The job was already stopped before we even got this far. Deal
			// with it in the same way as any other interruption.
			if (execution.getStatus() == BatchStatus.STOPPING) {
				throw new JobInterruptedException("JobExecution already stopped before being executed.");
			}

			execution.setStartTime(new Date());
			updateStatus(execution, BatchStatus.STARTING);
			
			listener.beforeJob(execution);

			int startedCount = 0;

			List steps = getSteps();
			for (Iterator i = steps.iterator(); i.hasNext();) {

				Step step = (Step) i.next();

				if (shouldStart(jobInstance, step)) {

					startedCount++;
					updateStatus(execution, BatchStatus.STARTED);
					StepExecution stepExecution = execution.createStepExecution(step);

					StepContext parentStepContext = StepSynchronizationManager.getContext();
					final StepContext stepContext = new SimpleStepContext(stepExecution, parentStepContext);
					StepSynchronizationManager.register(stepContext);
					try {
						step.execute(stepExecution);
					} finally {
						// clear any registered synchronizations
						StepSynchronizationManager.close();
					}

					status = stepExecution.getExitStatus();

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

			listener.afterJob();

		}
		catch (JobInterruptedException e) {
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
			execution.setEndTime(new Date());
			execution.setExitStatus(status);
			jobRepository.saveOrUpdate(execution);
		}

	}

	private void updateStatus(JobExecution jobExecution, BatchStatus status) {
		jobExecution.setStatus(status);
		jobRepository.saveOrUpdate(jobExecution);
	}

	/*
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 */
	private boolean shouldStart(JobInstance jobInstance, Step step) {

		BatchStatus stepStatus;
		// if the last execution is null, the step has never been executed.
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, step);
		if (lastStepExecution == null) {
			stepStatus = BatchStatus.STARTING;
		}
		else {
			stepStatus = lastStepExecution.getStatus();
		}

		if (stepStatus == BatchStatus.UNKNOWN) {
			throw new BatchCriticalException("Cannot restart step from UNKNOWN status.  "
					+ "The last execution ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed.  " + "Manual intervention is probably necessary.");
		}

		if (stepStatus == BatchStatus.COMPLETED && step.isAllowStartIfComplete() == false) {
			// step is complete, false should be returned, indicating that the
			// step should not be started
			return false;
		}

		if (jobRepository.getStepExecutionCount(jobInstance, step) < step.getStartLimit()) {
			// step start count is less than start max, return true
			return true;
		}
		else {
			// start max has been exceeded, throw an exception.
			throw new BatchCriticalException("Maximum start limit exceeded for step: " + step.getName()
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
	public void setExceptionClassifier(ExitStatusExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}
}
