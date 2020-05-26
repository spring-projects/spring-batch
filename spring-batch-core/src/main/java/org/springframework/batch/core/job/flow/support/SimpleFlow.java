/*
 * Copyright 2006-2014 the original author or authors.
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
package org.springframework.batch.core.job.flow.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutionException;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.State;
import org.springframework.beans.factory.InitializingBean;

/**
 * A {@link Flow} that branches conditionally depending on the exit status of
 * the last {@link State}. The input parameters are the state transitions (in no
 * particular order). The start state name can be specified explicitly (and must
 * exist in the set of transitions), or computed from the existing transitions,
 * if unambiguous.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @since 2.0
 */
public class SimpleFlow implements Flow, InitializingBean {

	private static final Log logger = LogFactory.getLog(SimpleFlow.class);

	private State startState;

	private Map<String, Set<StateTransition>> transitionMap = new HashMap<>();

	private Map<String, State> stateMap = new HashMap<>();

	private List<StateTransition> stateTransitions = new ArrayList<>();

	private final String name;

	private Comparator<StateTransition> stateTransitionComparator;

	public void setStateTransitionComparator(Comparator<StateTransition> stateTransitionComparator) {
		this.stateTransitionComparator = stateTransitionComparator;
	}

	/**
	 * Create a flow with the given name.
	 *
	 * @param name the name of the flow
	 */
	public SimpleFlow(String name) {
		this.name = name;
	}

	public State getStartState() {
		return this.startState;
	}

	/**
	 * Get the name for this flow.
	 *
	 * @see Flow#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Public setter for the stateTransitions.
	 *
	 * @param stateTransitions the stateTransitions to set
	 */
	public void setStateTransitions(List<StateTransition> stateTransitions) {

		this.stateTransitions = stateTransitions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public State getState(String stateName) {
		return stateMap.get(stateName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<State> getStates() {
		return new HashSet<>(stateMap.values());
	}

	/**
	 * Locate start state and pre-populate data structures needed for execution.
	 *
	 * @see InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (startState == null) {
			initializeTransitions();
		}
	}

	/**
	 * @see Flow#start(FlowExecutor)
	 */
	@Override
	public FlowExecution start(FlowExecutor executor) throws FlowExecutionException {
		if (startState == null) {
			initializeTransitions();
		}
		State state = startState;
		String stateName = state.getName();
		return resume(stateName, executor);
	}

	/**
	 * @see Flow#resume(String, FlowExecutor)
	 */
	@Override
	public FlowExecution resume(String stateName, FlowExecutor executor) throws FlowExecutionException {

		FlowExecutionStatus status = FlowExecutionStatus.UNKNOWN;
		State state = stateMap.get(stateName);

		if (logger.isDebugEnabled()) {
			logger.debug("Resuming state="+stateName+" with status="+status);
		}
		StepExecution stepExecution = null;

		// Terminate if there are no more states
		while (isFlowContinued(state, status, stepExecution)) {
			stateName = state.getName();

			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Handling state="+stateName);
				}
				status = state.handle(executor);
				stepExecution = executor.getStepExecution();
			}
			catch (FlowExecutionException e) {
				executor.close(new FlowExecution(stateName, status));
				throw e;
			}
			catch (Exception e) {
				executor.close(new FlowExecution(stateName, status));
				throw new FlowExecutionException(String.format("Ended flow=%s at state=%s with exception", name,
																	  stateName), e);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Completed state="+stateName+" with status="+status);
			}

			state = nextState(stateName, status, stepExecution);
		}

		FlowExecution result = new FlowExecution(stateName, status);
		executor.close(result);
		return result;

	}

	protected Map<String, Set<StateTransition>> getTransitionMap() {
		return transitionMap;
	}

	protected Map<String, State> getStateMap() {
		return stateMap;
	}

	/**
	 * @param stateName the name of the next state.
	 * @param status {@link FlowExecutionStatus} instance.
	 * @param stepExecution {@link StepExecution} instance.
	 * @return the next {@link Step} (or null if this is the end)
	 * @throws FlowExecutionException thrown if error occurs during nextState processing.
	 */
	protected State nextState(String stateName, FlowExecutionStatus status, StepExecution stepExecution) throws FlowExecutionException {
		Set<StateTransition> set = transitionMap.get(stateName);

		if (set == null) {
			throw new FlowExecutionException(String.format("No transitions found in flow=%s for state=%s", getName(),
																  stateName));
		}

		String next = null;
		String exitCode = status.getName();

		for (StateTransition stateTransition : set) {
			if (stateTransition.matches(exitCode) || (exitCode.equals("PENDING") && stateTransition.matches("STOPPED"))) {
				if (stateTransition.isEnd()) {
					// End of job
					return null;
				}
				next = stateTransition.getNext();
				break;
			}
		}

		if (next == null) {
			throw new FlowExecutionException(String.format("Next state not found in flow=%s for state=%s with exit status=%s", getName(), stateName, status.getName()));
		}

		if (!stateMap.containsKey(next)) {
			throw new FlowExecutionException(String.format("Next state not specified in flow=%s for next=%s",
																  getName(), next));
		}

		return stateMap.get(next);

	}

	protected boolean isFlowContinued(State state, FlowExecutionStatus status, StepExecution stepExecution) {
		boolean continued = true;

		continued = state != null && status!=FlowExecutionStatus.STOPPED;

		if(stepExecution != null) {
			Boolean reRun = (Boolean) stepExecution.getExecutionContext().get("batch.restart");
			Boolean executed = (Boolean) stepExecution.getExecutionContext().get("batch.executed");

			if((executed == null || !executed) && reRun != null && reRun && status == FlowExecutionStatus.STOPPED && !state.getName().endsWith(stepExecution.getStepName()) ) {
				continued = true;
			}
		}

		return continued;
	}

	private boolean stateNameEndsWithStepName(State state, StepExecution stepExecution) {
		return !(stepExecution == null || state == null) && !state.getName().endsWith(stepExecution.getStepName());
	}

	/**
	 * Analyse the transitions provided and generate all the information needed
	 * to execute the flow.
	 */
	private void initializeTransitions() {
		startState = null;
		transitionMap.clear();
		stateMap.clear();
		boolean hasEndStep = false;

		if (stateTransitions.isEmpty()) {
			throw new IllegalArgumentException("No start state was found. You must specify at least one step in a job.");
		}

		for (StateTransition stateTransition : stateTransitions) {
			State state = stateTransition.getState();
			String stateName = state.getName();
			stateMap.put(stateName, state);
		}

		for (StateTransition stateTransition : stateTransitions) {

			State state = stateTransition.getState();

			if (!stateTransition.isEnd()) {

				String next = stateTransition.getNext();

				if (!stateMap.containsKey(next)) {
					throw new IllegalArgumentException("Missing state for [" + stateTransition + "]");
				}

			}
			else {
				hasEndStep = true;
			}

			String name = state.getName();

			Set<StateTransition> set = transitionMap.get(name);
			if (set == null) {
				// If no comparator is provided, we will maintain the order of insertion
				if(stateTransitionComparator == null) {
					set = new LinkedHashSet<>();
				} else {
					set = new TreeSet<>(stateTransitionComparator);
				}

				transitionMap.put(name, set);
			}
			set.add(stateTransition);

		}

		if (!hasEndStep) {
			throw new IllegalArgumentException(
													  "No end state was found.  You must specify at least one transition with no next state.");
		}

		startState = stateTransitions.get(0).getState();

	}
}
