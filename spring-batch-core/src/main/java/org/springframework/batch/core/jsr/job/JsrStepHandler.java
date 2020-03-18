/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.jsr.job;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Extends {@link SimpleStepHandler} to apply JSR-352 specific logic for whether to
 * start a step.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrStepHandler extends SimpleStepHandler {

	private static final Log logger = LogFactory.getLog(JsrStepHandler.class);

	private JobExplorer jobExplorer;

	/**
	 * @param jobRepository instance of {@link JobRepository}.
	 * @param jobExplorer instance of {@link JobExplorer}.
	 */
	public JsrStepHandler(JobRepository jobRepository, JobExplorer jobExplorer) {
		super(jobRepository, new ExecutionContext());
		this.jobExplorer = jobExplorer;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(jobExplorer != null, "A JobExplorer must be provided");
	}


	/**
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 * @param lastStepExecution the last step execution
	 * @param jobExecution instance of {@link JobExecution}
	 * @param step instance of {@link Step}
	 *
	 * @throws StartLimitExceededException if the start limit has been exceeded
	 * for this step
	 * @throws JobRestartException if the job is in an inconsistent state from
	 * an earlier failure
	 */
	@Override
	protected boolean shouldStart(StepExecution lastStepExecution, JobExecution jobExecution, Step step)
			throws JobRestartException, StartLimitExceededException {
		BatchStatus stepStatus;
		String restartStep = null;
		if (lastStepExecution == null) {
			jobExecution.getExecutionContext().put("batch.startedStep", step.getName());
			stepStatus = BatchStatus.STARTING;
		}
		else {
			stepStatus = lastStepExecution.getStatus();

			JobExecution lastJobExecution = getLastJobExecution(jobExecution);

			if(lastJobExecution.getExecutionContext().containsKey("batch.restartStep")) {
				restartStep = lastJobExecution.getExecutionContext().getString("batch.restartStep");

				if(CollectionUtils.isEmpty(jobExecution.getStepExecutions()) && lastJobExecution.getStatus() == BatchStatus.STOPPED && StringUtils.hasText(restartStep)) {
					if(!restartStep.equals(step.getName()) && !jobExecution.getExecutionContext().containsKey("batch.startedStep")) {
						logger.info("Job was stopped and should restart at step " + restartStep + ".  The current step is " + step.getName());
						return false;
					} else {
						// Indicates the starting point for execution evaluation per JSR-352
						jobExecution.getExecutionContext().put("batch.startedStep", step.getName());
					}
				}
			}
		}

		if (stepStatus == BatchStatus.UNKNOWN) {
			throw new JobRestartException("Cannot restart step from UNKNOWN status. "
					+ "The last execution ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed. Manual intervention is probably necessary.");
		}

		if ((stepStatus == BatchStatus.COMPLETED && step.isAllowStartIfComplete() == false)
				|| stepStatus == BatchStatus.ABANDONED) {
			// step is complete, false should be returned, indicating that the
			// step should not be started
			logger.info("Step already complete or not restartable, so no action to execute: " + lastStepExecution);
			return false;
		}

		if (getJobRepository().getStepExecutionCount(jobExecution.getJobInstance(), step.getName()) < step.getStartLimit()) {
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
	 * Since all JSR-352 jobs are run asynchronously, {@link JobRepository#getLastJobExecution(String, org.springframework.batch.core.JobParameters)}
	 * could return the currently running {@link JobExecution}.  To get around this, we use the {@link JobExplorer}
	 * to get a list of the executions and get the most recent one that is <em>not</em> the currently running
	 * {@link JobExecution}.
	 *
	 * @param jobExecution
	 * @return the last executed JobExecution.
	 */
	private JobExecution getLastJobExecution(JobExecution jobExecution) {
		List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobExecution.getJobInstance());
		JobExecution lastJobExecution = null;

		for (JobExecution curJobExecution : jobExecutions) {
			if(lastJobExecution == null && curJobExecution.getId().longValue() != jobExecution.getId().longValue()) {
				lastJobExecution = curJobExecution;
			} else if(curJobExecution.getId().longValue() != jobExecution.getId().longValue() && (lastJobExecution == null || curJobExecution.getId().longValue() > lastJobExecution.getId().longValue())) {
				lastJobExecution = curJobExecution;
			}
		}
		return lastJobExecution;
	}
}
