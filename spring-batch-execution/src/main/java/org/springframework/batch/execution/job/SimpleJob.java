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

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.listener.CompositeJobListener;
import org.springframework.batch.io.exception.InfrastructureException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simple implementation of (@link Job} interface providing the ability to run a
 * {@link JobExecution}. Sequentially executes a job by iterating through its
 * list of steps.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleJob extends AbstractJob {

	private JobRepository jobRepository;

	private CompositeJobListener listener = new CompositeJobListener();

	/**
	 * Public setter for injecting {@link JobListener}s. They will all be given
	 * the {@link JobListener} callbacks at the appropriate point in the job.
	 * 
	 * @param listeners the listeners to set.
	 */
	public void setJobListeners(JobListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			this.listener.register(listeners[i]);
		}
	}
	
	/**
	 * Register a single listener for the {@link JobListener} callbacks.
	 * @param listener a {@link JobListener}
	 */
	public void registerListener(JobListener listener) {
		this.listener.register(listener);
	}

	/**
	 * Run the specified job by looping through the steps and delegating to the
	 * {@link Step}.
	 * 
	 * @see org.springframework.batch.core.Job#execute(org.springframework.batch.core.JobExecution)
	 */
	public void execute(JobExecution execution) throws InfrastructureException {

		JobInstance jobInstance = execution.getJobInstance();

		StepExecution currentStepExecution = null;
		int startedCount = 0;
		List steps = getSteps();

		try {

			// The job was already stopped before we even got this far. Deal
			// with it in the same way as any other interruption.
			if (execution.getStatus() == BatchStatus.STOPPING) {
				throw new JobInterruptedException("JobExecution already stopped before being executed.");
			}

			execution.setStartTime(new Date());
			updateStatus(execution, BatchStatus.STARTING);

			listener.beforeJob(execution);

			for (Iterator i = steps.iterator(); i.hasNext();) {

				Step step = (Step) i.next();

				if (shouldStart(jobInstance, step)) {

					startedCount++;
					updateStatus(execution, BatchStatus.STARTED);
					currentStepExecution = execution.createStepExecution(step);

					step.execute(currentStepExecution);

				}
			}

			updateStatus(execution, BatchStatus.COMPLETED);

			listener.afterJob(execution);

		}
		catch (JobInterruptedException e) {
			execution.setStatus(BatchStatus.STOPPED);
			rethrow(e);
		}
		catch (Throwable t) {
			execution.setStatus(BatchStatus.FAILED);
			rethrow(t);
		}
		finally {
			ExitStatus status = ExitStatus.FAILED;
			if (startedCount == 0) {
				if (steps.size() > 0) {
					status = ExitStatus.NOOP
							.addExitDescription("All steps already completed.  No processing was done.");
				}
				else {
					status = ExitStatus.NOOP.addExitDescription("No steps configured for this job.");
				}
			}
			else if (currentStepExecution != null) {
				status = currentStepExecution.getExitStatus();
			}

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
			throw new InfrastructureException("Cannot restart step from UNKNOWN status.  "
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
			throw new InfrastructureException("Maximum start limit exceeded for step: " + step.getName() + "StartMax: "
					+ step.getStartLimit());
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
			throw new InfrastructureException(t);
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
}
