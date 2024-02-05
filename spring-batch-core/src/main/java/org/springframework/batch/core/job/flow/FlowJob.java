/*
 * Copyright 2006-2023 the original author or authors.
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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.builder.AlreadyUsedStepNameException;
import org.springframework.batch.core.step.StepHolder;
import org.springframework.batch.core.step.StepLocator;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the {@link Job} interface that allows for complex flows of steps,
 * rather than requiring sequential execution. In general, this job implementation was
 * designed to be used behind a parser, allowing for a namespace to abstract away details.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 2.0
 */
public class FlowJob extends AbstractJob {

	protected Flow flow;

	private final Map<String, Step> stepMap = new ConcurrentHashMap<>();

	private volatile boolean initialized = false;

	/**
	 * Create a {@link FlowJob} with null name and no flow (invalid state).
	 */
	public FlowJob() {
		super();
	}

	/**
	 * Create a {@link FlowJob} with provided name and no flow (invalid state).
	 * @param name the name to be associated with the FlowJob.
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
	 * {@inheritDoc}
	 */
	@Override
	public Step getStep(String stepName) {
		init();
		return stepMap.get(stepName);
	}


	/**
	 * Initialize the step names
	 */
	private void init() {
		if (!initialized) {
			findStepsThrowingIfNameNotUnique(flow, stepMap);
			initialized = true;
		}
	}

	private void findStepsThrowingIfNameNotUnique(Flow flow, Map<String, Step> map) {

		for (State state : flow.getStates()) {
			if (state instanceof StepLocator locator) {
				for (String name : locator.getStepNames()) {
					addToMapCheckingUnicity(map, locator.getStep(name), name);
				}
			}
			//TODO remove this else bock ? not executed during tests : the only State wich implements StepHolder is StepState which implements also StepLocator
			/*
			Tests Coverage
			Hits : 30
				state instanceof StepHolder
					true hits: 0
					false hits : 30
			*/
			else if (state instanceof StepHolder stepHolder) {
				Step step = stepHolder.getStep();
				addToMapCheckingUnicity(map, step, step.getName());
			}
			else if (state instanceof FlowHolder flowHolder) {
				for (Flow subflow : flowHolder.getFlows()) {
					findStepsThrowingIfNameNotUnique(subflow, map);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getStepNames() {
		init();
		return stepMap.keySet();
	}

	/**
	 * @see AbstractJob#doExecute(JobExecution)
	 */
	@Override
	protected void doExecute(final JobExecution execution) throws JobExecutionException {
		try {
			JobFlowExecutor executor = new JobFlowExecutor(getJobRepository(),
					new SimpleStepHandler(getJobRepository()), execution);
			executor.updateJobExecutionStatus(flow.start(executor).getStatus());
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}

	@Override
	protected void checkStepNamesUnicity() throws AlreadyUsedStepNameException {
		init();
	}

}
