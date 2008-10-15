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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.repeat.ExitStatus;

/**
 * A {@link Job} that branches conditionally depending on the exit status of the
 * last step execution. The input parameters are the step transitions (in no
 * particular order). The start step name must also be specified (and must exist
 * in the set of transitions).
 * 
 * @author Dave Syer
 * 
 */
public class ConditionalJob extends AbstractJob {

	private Step startStep;

	private Map<String, SortedSet<StepTransition>> transitionMap = new HashMap<String, SortedSet<StepTransition>>();

	private Map<String, Step> stepMap = new HashMap<String, Step>();

	private String startStepName;

	private Collection<StepTransition> stepTransitions;

	/**
	 * Create a {@link Job} with the given name.
	 * @param name
	 */
	public ConditionalJob(String name) {
		super(name);
	}

	/**
	 * Create a {@link Job} with no name.
	 */
	public ConditionalJob() {
		super();
	}

	/**
	 * Public setter for the start step name.
	 * @param startStepName the name of the start step
	 */
	public void setStartStepName(String startStepName) {
		this.startStepName = startStepName;
	}

	/**
	 * Public setter for the stepTransitions.
	 * @param stepTransitions the stepTransitions to set
	 */
	public void setStepTransitions(Collection<StepTransition> stepTransitions) {

		this.stepTransitions = stepTransitions;
	}

	/**
	 * Locate start step and pre-populate data structures needed for execution.
	 * 
	 * @see AbstractJob#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		super.afterPropertiesSet();

		startStep = null;
		transitionMap.clear();
		stepMap.clear();
		boolean hasEndStep = false;

		for (StepTransition stepTransition : stepTransitions) {
			Step step = stepTransition.getStep();
			stepMap.put(step.getName(), step);
		}

		for (StepTransition stepTransition : stepTransitions) {

			Step step = stepTransition.getStep();

			if (!stepTransition.isEnd()) {

				String next = stepTransition.getNext();

				if (!stepMap.containsKey(next)) {
					throw new IllegalArgumentException("Missing step for [" + stepTransition + "]");
				}

			}
			else {
				hasEndStep = true;
			}

			String name = step.getName();

			SortedSet<StepTransition> set = transitionMap.get(name);
			if (set == null) {
				set = new TreeSet<StepTransition>();
				transitionMap.put(name, set);
			}
			set.add(stepTransition);

		}

		if (!hasEndStep) {
			throw new IllegalArgumentException(
					"No end step was found.  You must specify at least one transition with no next step.");
		}

		if (startStepName != null) {

			startStep = stepMap.get(startStepName);
			if (startStep == null) {
				throw new IllegalArgumentException(
						"Start step does not exist (if you specify a startStepName make sure "
								+ "a step with that name is in one of the transitions): [" + startStepName + "]");
			}

		}
		else {

			// Try and locate a transition with no incoming links

			Set<String> nextStepNames = new HashSet<String>();

			for (StepTransition stepTransition : stepTransitions) {
				nextStepNames.add(stepTransition.getNext());
			}

			for (StepTransition stepTransition : stepTransitions) {
				Step step = stepTransition.getStep();
				if (!nextStepNames.contains(step.getName())) {
					if (startStep != null && !startStep.getName().equals(step.getName())) {
						throw new IllegalArgumentException(String.format(
								"Multiple possible start steps found: [%s, %s].  "
										+ "Please specify one explicitly with the startStepName property.", startStep
										.getName(), step.getName()));
					}
					startStep = step;
				}
			}

			if (startStep == null) {
				throw new IllegalArgumentException(
						"No start step could be located (no transition without incoming links)");
			}

		}

	}

	/**
	 * @see AbstractJob#doExecute(JobExecution)
	 * @throws JobExecutionException if the next step cannot be located at any
	 * point
	 */
	@Override
	protected StepExecution doExecute(JobExecution jobExecution) throws JobExecutionException {
		StepExecution stepExecution = null;
		Step step = nextStep(null);
		while (step != null) {
			stepExecution = handleStep(step, jobExecution);
			step = nextStep(stepExecution);
		}
		return stepExecution;
	}

	/**
	 * @param stepExecution the last {@link StepExecution} (or null if this is
	 * the start)
	 * @return the next {@link Step} (or null if this is the end)
	 * @throws JobExecutionException
	 */
	private Step nextStep(StepExecution stepExecution) throws JobExecutionException {

		if (stepExecution == null) {
			return startStep;
		}

		String stepName = stepExecution.getStepName();
		Set<StepTransition> set = transitionMap.get(stepName);

		if (set == null) {
			throw new JobExecutionException(String.format("No transitions found in job=%s for step=%s", getName(),
					stepName));
		}

		ExitStatus status = stepExecution.getExitStatus();
		String next = null;
		for (StepTransition stepTransition : set) {
			if (stepTransition.matches(status)) {
				if (stepTransition.isEnd()) {
					// End of job
					return null;
				}
				next = stepTransition.getNext();
				break;
			}
		}

		if (next == null) {
			throw new JobExecutionException(String.format(
					"Next step not found in job=%s for step=%s with exit status=%s", getName(), stepName, status));
		}

		if (!stepMap.containsKey(next)) {
			throw new JobExecutionException(String.format("Next step not specified in job=%s for next=%s", getName(),
					next));
		}

		return stepMap.get(next);

	}

}
