/*
 * Copyright 2009-2012 the original author or authors.
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

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.StepHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.util.Assert;

/**
 * A {@link Step} implementation that delegates to a {@link Flow}. Useful for
 * logical grouping of steps, and especially for partitioning with multiple
 * steps per execution. If the flow has steps then when the {@link FlowStep}
 * executes, all steps including the parent {@link FlowStep} will have
 * executions in the {@link JobRepository} (one for the parent and one each for
 * the flow steps).
 * 
 * @author Dave Syer
 * 
 */
public class FlowStep extends AbstractStep {

	private Flow flow;

	/**
	 * Default constructor convenient for configuration purposes.
	 */
	public FlowStep() {
		super(null);
	}

	/**
	 * Constructor for a {@link FlowStep} that sets the flow and of the step
	 * explicitly.
	 *
	 * @param flow the {@link Flow} instance to be associated with this step.
	 */
	public FlowStep(Flow flow) {
		super(flow.getName());
	}

	/**
	 * Public setter for the flow.
	 * 
	 * @param flow the flow to set
	 */
	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	/**
	 * Ensure that the flow is set.
	 * @see AbstractStep#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(flow != null, "A Flow must be provided");
		if (getName()==null) {
			setName(flow.getName());
		}
		super.afterPropertiesSet();
	}

	/**
	 * Delegate to the flow provided for the execution of the step.
	 * 
	 * @see AbstractStep#doExecute(StepExecution)
	 */
	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		try {
			stepExecution.getExecutionContext().put(STEP_TYPE_KEY, this.getClass().getName());
			StepHandler stepHandler = new SimpleStepHandler(getJobRepository(), stepExecution.getExecutionContext());
			FlowExecutor executor = new JobFlowExecutor(getJobRepository(), stepHandler, stepExecution.getJobExecution());
			executor.updateJobExecutionStatus(flow.start(executor).getStatus());
			stepExecution.upgradeStatus(executor.getJobExecution().getStatus());
			stepExecution.setExitStatus(executor.getJobExecution().getExitStatus());
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}

}
