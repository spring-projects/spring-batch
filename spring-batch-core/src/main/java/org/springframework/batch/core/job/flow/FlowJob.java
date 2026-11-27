/*
 * Copyright 2006-present the original author or authors.
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.core.job.Job;

import org.jspecify.annotations.Nullable;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.step.ListableStepLocator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.StepHandler;
import org.springframework.batch.core.step.StepHolder;
import org.springframework.batch.core.step.StepLocator;

/**
 * Implementation of the {@link Job} interface that allows for complex flows of steps,
 * rather than requiring sequential execution. In general, this job implementation was
 * designed to be used behind a parser, allowing for a namespace to abstract away details.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @author Taehun Lee
 * @since 2.0
 */
public class FlowJob extends AbstractJob {

	protected Flow flow;

	private final Map<String, Step> stepMap = new ConcurrentHashMap<>();

	private volatile boolean initialized = false;

	private volatile boolean duplicateStepNamesChecked = false;

	// scoped steps are only added when the flow reaches them
	private final Map<String, Step> stepsByName = new ConcurrentHashMap<>();

	private final Set<String> reportedDuplicateStepNames = ConcurrentHashMap.newKeySet();

	/**
	 * Create a {@link FlowJob} with null name and no flow (invalid state).
	 * @deprecated since 6.0 for removal in 7.0. Use {@link #FlowJob(String)} instead.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public FlowJob() {
		this(null);
	}

	/**
	 * Create a {@link FlowJob} with provided name and no flow (invalid state).
	 * @param name the name to be associated with the FlowJob.
	 */
	public FlowJob(String name) {
		super(name);
	}

	private void warnOnDuplicateStepNames() {
		if (!this.duplicateStepNamesChecked) {
			this.duplicateStepNamesChecked = true;
			// the check only produces a warning and must not fail the job
			try {
				for (String name : findDuplicateStepNames()) {
					warnOnDuplicateStepName(name);
				}
			}
			catch (RuntimeException e) {
				logger.debug("Could not check the step names of job [" + getName() + "] for duplicates", e);
			}
		}
	}

	private void warnIfScopedStepNameIsDuplicate(Step step) {
		if (step instanceof ScopedObject) {
			// the check only produces a warning and must not fail the job
			try {
				String name = step.getName();
				Step previous = this.stepsByName.putIfAbsent(name, step);
				if (previous != null && previous != step) {
					warnOnDuplicateStepName(name);
				}
			}
			catch (RuntimeException e) {
				logger.debug("Could not check the name of a scoped step of job [" + getName() + "] for duplicates", e);
			}
		}
	}

	private void warnOnDuplicateStepName(String name) {
		if (this.reportedDuplicateStepNames.add(name)) {
			logger.warn("Step name [" + name + "] is used by more than one step in job [" + getName()
					+ "]. Step names should be unique within a job: restart relies on them.");
		}
	}

	/**
	 * Find the names shared by distinct steps of the flow (including nested flows). The
	 * same step instance used several times in the flow is not considered a duplicate.
	 * Scoped steps are not checked here, as resolving their name would create them before
	 * the flow reaches them and break the late binding of their attributes. They are
	 * checked when the flow executes them.
	 * @return the duplicate step names, empty if all step names are unique
	 * @since 6.1
	 */
	Set<String> findDuplicateStepNames() {
		Set<String> duplicates = new LinkedHashSet<>();
		findSteps(this.flow, this.stepsByName, duplicates, false);
		return duplicates;
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
	public @Nullable Step getStep(String stepName) {
		if (!initialized) {
			init();
		}
		return stepMap.get(stepName);
	}

	/**
	 * Initialize the step names
	 */
	private void init() {
		findSteps(flow, stepMap, new HashSet<>(), true);
		initialized = true;
	}

	private void findSteps(Flow flow, Map<String, Step> map, Set<String> duplicates, boolean includeScopedSteps) {

		for (State state : flow.getStates()) {
			if (!includeScopedSteps && state instanceof StepHolder stepHolder
					&& stepHolder.getStep() instanceof ScopedObject) {
				continue;
			}
			if (state instanceof ListableStepLocator locator) {
				for (String name : locator.getStepNames()) {
					register(map, name, locator.getStep(name), duplicates);
				}
			}
			else if (state instanceof StepHolder stepHolder) {
				Step step = stepHolder.getStep();
				register(map, step.getName(), step, duplicates);
			}
			else if (state instanceof FlowHolder flowHolder) {
				for (Flow subflow : flowHolder.getFlows()) {
					findSteps(subflow, map, duplicates, includeScopedSteps);
				}
			}
		}

	}

	private void register(Map<String, Step> map, String name, Step step, Set<String> duplicates) {
		Step previous = map.put(name, step);
		// the same step instance can legitimately appear several times in a flow
		if (previous != null && previous != step) {
			duplicates.add(name);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getStepNames() {
		if (!initialized) {
			init();
		}
		return stepMap.keySet();
	}

	/**
	 * @see AbstractJob#doExecute(JobExecution)
	 */
	@Override
	protected void doExecute(JobExecution execution) throws JobExecutionException {
		// scoped steps are checked only when the flow reaches them: getting their name
		// earlier would create them before their late bound attributes are available
		warnOnDuplicateStepNames();
		StepHandler stepHandler = new SimpleStepHandler(getJobRepository());
		try {
			JobFlowExecutor executor = new JobFlowExecutor(getJobRepository(), (step, jobExecution) -> {
				warnIfScopedStepNameIsDuplicate(step);
				return stepHandler.handleStep(step, jobExecution);
			}, execution);
			executor.updateJobExecutionStatus(flow.start(executor).getStatus());
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}

}
