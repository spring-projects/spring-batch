/*
 * Copyright 2006-2018 the original author or authors.
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

package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.StepHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link FlowExecutor} for use in components that need to
 * execute a flow related to a {@link JobExecution}.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
public class JobFlowExecutor implements FlowExecutor {

	private final ThreadLocal<StepExecution> stepExecutionHolder = new ThreadLocal<>();

	private final JobExecution execution;

	protected ExitStatus exitStatus = ExitStatus.EXECUTING;

	private final StepHandler stepHandler;

	private final JobRepository jobRepository;

	/**
	 * @param jobRepository instance of {@link JobRepository}.
	 * @param stepHandler instance of {@link StepHandler}.
	 * @param execution instance of {@link JobExecution}.
	 */
	public JobFlowExecutor(JobRepository jobRepository, StepHandler stepHandler, JobExecution execution) {
		this.jobRepository = jobRepository;
		this.stepHandler = stepHandler;
		this.execution = execution;
		stepExecutionHolder.set(null);
	}

	@Override
	public String executeStep(Step step) throws JobInterruptedException, JobRestartException,
	StartLimitExceededException {
		boolean isRerun = isStepRestart(step);
		StepExecution stepExecution = stepHandler.handleStep(step, execution);
		stepExecutionHolder.set(stepExecution);

		if (stepExecution == null) {
			return  ExitStatus.COMPLETED.getExitCode();
		}
		if (stepExecution.isTerminateOnly()) {
			throw new JobInterruptedException("Step requested termination: "+stepExecution, stepExecution.getStatus());
		}

		if(isRerun) {
			stepExecution.getExecutionContext().put("batch.restart", true);
		}

		return stepExecution.getExitStatus().getExitCode();
	}

	private boolean isStepRestart(Step step) {
		int count = jobRepository.getStepExecutionCount(execution.getJobInstance(), step.getName());

		return count > 0;
	}

	@Override
	public void abandonStepExecution() {
		StepExecution lastStepExecution = stepExecutionHolder.get();
		if (lastStepExecution != null && lastStepExecution.getStatus().isGreaterThan(BatchStatus.STOPPING)) {
			lastStepExecution.upgradeStatus(BatchStatus.ABANDONED);
			jobRepository.update(lastStepExecution);
		}
	}

	@Override
	public void updateJobExecutionStatus(FlowExecutionStatus status) {
		execution.setStatus(findBatchStatus(status));
		exitStatus = exitStatus.and(new ExitStatus(status.getName()));
		execution.setExitStatus(exitStatus);
	}

	@Override
	public JobExecution getJobExecution() {
		return execution;
	}

	@Override
	@Nullable
	public StepExecution getStepExecution() {
		return stepExecutionHolder.get();
	}

	@Override
	public void close(FlowExecution result) {
		stepExecutionHolder.set(null);
	}

	@Override
	public boolean isRestart() {
		if (getStepExecution() != null && getStepExecution().getStatus() == BatchStatus.ABANDONED) {
			/*
			 * This is assumed to be the last step execution and it was marked
			 * abandoned, so we are in a restart of a stopped step.
			 */
			// TODO: mark the step execution in some more definitive way?
			return true;
		}
		return execution.getStepExecutions().isEmpty();
	}

	@Override
	public void addExitStatus(String code) {
		exitStatus = exitStatus.and(new ExitStatus(code));
	}

	/**
	 * @param status {@link FlowExecutionStatus} to convert.
	 * @return A {@link BatchStatus} appropriate for the {@link FlowExecutionStatus} provided
	 */
	protected BatchStatus findBatchStatus(FlowExecutionStatus status) {
		for (BatchStatus batchStatus : BatchStatus.values()) {
			if (status.getName().startsWith(batchStatus.toString())) {
				return batchStatus;
			}
		}
		return BatchStatus.UNKNOWN;
	}

}
