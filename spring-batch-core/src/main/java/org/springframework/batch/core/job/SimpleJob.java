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

package org.springframework.batch.core.job;

import java.util.Date;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simple implementation of {@link Job} interface providing the ability to run a
 * {@link JobExecution}. Sequentially executes a job by iterating through its
 * list of steps.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleJob extends AbstractJob {

	/**
	 * Run the specified job by looping through the steps and delegating to the
	 * {@link Step}.
	 * 
	 * @see org.springframework.batch.core.Job#execute(org.springframework.batch.core.JobExecution)
	 * @throws StartLimitExceededException if start limit of one of the steps
	 * was exceeded
	 */
	public void execute(JobExecution execution) throws JobExecutionException {

		JobInstance jobInstance = execution.getJobInstance();

		StepExecution currentStepExecution = null;
		int startedCount = 0;
		List<Step> steps = getSteps();

		try {

			// The job was already stopped before we even got this far. Deal
			// with it in the same way as any other interruption.
			if (execution.getStatus() == BatchStatus.STOPPING) {
				throw new JobInterruptedException("JobExecution already stopped before being executed.");
			}

			execution.setStartTime(new Date());
			updateStatus(execution, BatchStatus.STARTING);

			getCompositeListener().beforeJob(execution);

			for (Step step : steps) {

				if (execution.getStatus() == BatchStatus.STOPPING) {
					throw new JobInterruptedException("JobExecution interrupted.");
				}

				if (shouldStart(jobInstance, step)) {

					startedCount++;
					updateStatus(execution, BatchStatus.STARTED);
					currentStepExecution = execution.createStepExecution(step);

					StepExecution lastStepExecution = getJobRepository().getLastStepExecution(jobInstance, step);

					boolean isRestart = (lastStepExecution != null && !lastStepExecution.getStatus().equals(
							BatchStatus.COMPLETED)) ? true : false;

					if (isRestart) {
						currentStepExecution.setExecutionContext(lastStepExecution.getExecutionContext());
					}
					else {
						currentStepExecution.setExecutionContext(new ExecutionContext());
					}

					step.execute(currentStepExecution);

				}
			}

			// Need to check again for stopped job
			if (execution.getStatus() == BatchStatus.STOPPING) {
				throw new JobInterruptedException("JobExecution interrupted.");
			}

			updateStatus(execution, BatchStatus.COMPLETED);

			getCompositeListener().afterJob(execution);

		}
		catch (JobInterruptedException e) {
			execution.setStatus(BatchStatus.STOPPED);
			getCompositeListener().onInterrupt(execution);
			rethrow(e);
		}
		catch (Throwable t) {
			execution.setStatus(BatchStatus.FAILED);
			getCompositeListener().onError(execution, t);
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
			getJobRepository().update(execution);
		}

	}

	private void updateStatus(JobExecution jobExecution, BatchStatus status) {
		jobExecution.setStatus(status);
		getJobRepository().update(jobExecution);
	}

	/*
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 */
	private boolean shouldStart(JobInstance jobInstance, Step step) throws JobExecutionException {

		BatchStatus stepStatus;
		// if the last execution is null, the step has never been executed.
		StepExecution lastStepExecution = getJobRepository().getLastStepExecution(jobInstance, step);
		if (lastStepExecution == null) {
			stepStatus = BatchStatus.STARTING;
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

		if (getJobRepository().getStepExecutionCount(jobInstance, step) < step.getStartLimit()) {
			// step start count is less than start max, return true
			return true;
		}
		else {
			// start max has been exceeded, throw an exception.
			throw new StartLimitExceededException("Maximum start limit exceeded for step: " + step.getName()
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
		else if (t instanceof Error) {
			throw (Error) t;
		}
		else {
			throw new UnexpectedJobExecutionException("Unexpected checked exception in job execution", t);
		}
	}
}
