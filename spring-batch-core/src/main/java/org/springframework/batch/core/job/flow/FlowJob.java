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
package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.util.Assert;


/**
 * @author Dave Syer
 * 
 */
public class FlowJob extends AbstractJob {

	private Flow flow;
	
	/**
	 * Create a {@link FlowJob} with null name and no flow (invalid state).
	 */
	public FlowJob() {
		super();
	}

	/**
	 * Create a {@link FlowJob} with provided name and no flow (invalid state).
	 */
	public FlowJob(String name) {
		super(name);
	}

	/**
	 * Public setter for the flow.
	 * @param flow the flow to set
	 */
	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	/**
	 * @see AbstractJob#doExecute(JobExecution)
	 */
	@Override
	protected StepExecution doExecute(final JobExecution execution) throws JobExecutionException {
		try {
			FlowExecution result = flow.start(new JobFlowExecutor(execution));
			return getLastStepExecution(execution, result);
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}

	/**
	 * @param execution the current {@link JobExecution}
	 * @param result the result of the flow execution
	 * @return a {@link StepExecution} with matching properties to the result
	 */
	private StepExecution getLastStepExecution(JobExecution execution, FlowExecution result) {
		StepExecution value = null;
		StepExecution backup = null;
		for (StepExecution stepExecution : execution.getStepExecutions()) {
			if (stepExecution.getStepName().equals(result.getName())
					&& stepExecution.getExitStatus().getExitCode().equals(result.getStatus())) {
				value = stepExecution;
			}
			if (isLater(backup,stepExecution)) {
				backup = stepExecution;
			}
		}
		if (value==null) {
			value = backup;
		}
		Assert.state(value != null, String.format(
				"Could not locate step execution matching expected properties: flowExecution=%s, stepExecutions=%s",
				result, execution.getStepExecutions()));
		return value;
	}

	/**
	 * @param first
	 * @param second
	 * @return true if the first is deemed to be executed after the second
	 */
	private boolean isLater(StepExecution first, StepExecution second) {
		if (first==null) {
			return true;
		}
		if (first.getEndTime()==null) {
			return first.getStartTime().after(second.getStartTime());
		}
		if (second.getEndTime()==null) {
			return false;
		}
		return first.getEndTime().after(second.getEndTime());
	}

	/**
	 * @author Dave Syer
	 *
	 */
	private class JobFlowExecutor implements FlowExecutor {

		private final ThreadLocal<StepExecution> stepExecutionHolder = new ThreadLocal<StepExecution>();
		private final JobExecution execution;

		/**
		 * @param execution
		 */
		private JobFlowExecutor(JobExecution execution) {
			this.execution = execution;
			stepExecutionHolder.set(null);
		}

		public String executeStep(Step step) throws JobInterruptedException, JobRestartException, StartLimitExceededException {
			StepExecution stepExecution = handleStep(step, execution);
			stepExecutionHolder.set(stepExecution);
			return stepExecution==null ? FlowExecution.COMPLETED : stepExecution.getExitStatus().getExitCode();
		}

		public JobExecution getJobExecution() {
			return execution;
		}
		
		public StepExecution getStepExecution() {
			return stepExecutionHolder.get();
		}
		
		public void close(FlowExecution result) {
			stepExecutionHolder.set(null);
		}

	}

}
