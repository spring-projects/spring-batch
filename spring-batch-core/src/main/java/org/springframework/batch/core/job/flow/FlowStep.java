/*
 * Copyright 2009-2026 the original author or authors.
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.step.ListableStepLocator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.StepHolder;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.StepHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.util.Assert;

/**
 * A {@link Step} implementation that delegates to a {@link Flow}. Useful for logical
 * grouping of steps, and especially for partitioning with multiple steps per execution.
 * If the flow has steps then when the {@link FlowStep} executes, all steps including the
 * parent {@link FlowStep} will have executions in the {@link JobRepository} (one for the
 * parent and one each for the flow steps).
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Seonghun Lee
 *
 */
public class FlowStep extends AbstractStep implements ListableStepLocator {

	private Flow flow;

	/**
	 * Create a new instance of a {@link FlowStep} with the given job repository.
	 * @param jobRepository the job repository to use. Must not be null.
	 * @since 6.0
	 */
	public FlowStep(JobRepository jobRepository) {
		super(jobRepository);
	}

	/**
	 * Constructor for a {@link FlowStep} that sets the flow and of the step explicitly.
	 * @param flow the {@link Flow} instance to be associated with this step.
	 */
	public FlowStep(Flow flow) {
		super(flow.getName());
	}

	/**
	 * Public setter for the flow.
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
		if (getName() == null) {
			setName(flow.getName());
		}
		super.afterPropertiesSet();
	}

	/**
	 * Retrieve the step with the given name from the flow, looking into any nested flows
	 * and flow steps.
	 * @param stepName the name of the step to retrieve
	 * @return the step with the given name, or {@code null} if no such step exists
	 * @since 6.1
	 */
	@Override
	public @Nullable Step getStep(String stepName) {
		return findSteps().get(stepName);
	}

	/**
	 * Retrieve the names of the steps defined in the flow, including the ones defined in
	 * nested flows and flow steps.
	 * @return the names of the steps in the flow
	 * @since 6.1
	 */
	@Override
	public Collection<String> getStepNames() {
		return findSteps().keySet();
	}

	private Map<String, Step> findSteps() {
		Map<String, Step> steps = new LinkedHashMap<>();
		findSteps(this.flow, steps);
		return steps;
	}

	private static void findSteps(Flow flow, Map<String, Step> map) {
		for (State state : flow.getStates()) {
			if (state instanceof ListableStepLocator locator) {
				for (String name : locator.getStepNames()) {
					map.put(name, locator.getStep(name));
				}
			}
			else if (state instanceof StepHolder stepHolder) {
				Step step = stepHolder.getStep();
				map.put(step.getName(), step);
			}
			else if (state instanceof FlowHolder flowHolder) {
				for (Flow subflow : flowHolder.getFlows()) {
					findSteps(subflow, map);
				}
			}
		}
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
			JobFlowExecutor executor = new JobFlowExecutor(getJobRepository(), stepHandler,
					stepExecution.getJobExecution());
			FlowExecutionStatus status = flow.start(executor).getStatus();
			BatchStatus batchStatus = executor.findBatchStatus(status);
			stepExecution.upgradeStatus(batchStatus);
			stepExecution.setExitStatus(executor.exitStatus.and(new ExitStatus(status.getName())));
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}

}
