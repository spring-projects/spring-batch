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
package org.springframework.batch.core.job.flow.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutionException;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.beans.factory.InitializingBean;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath.Step;

/**
 * A {@link Flow} that branches conditionally depending on the exit status of
 * the last {@link State}. The input parameters are the state transitions (in no
 * particular order). The start state name can be specified explicitly (and must
 * exist in the set of transitions), or computed from the existing transitions,
 * if unambiguous.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleFlow implements Flow, InitializingBean {

	private State startState;

	private Map<String, SortedSet<StateTransition>> transitionMap = new HashMap<String, SortedSet<StateTransition>>();

	private Map<String, State> stateMap = new HashMap<String, State>();

	private String startStateName;

	private Collection<StateTransition> stateTransitions = new HashSet<StateTransition>();

	private final String name;

	/**
	 * Create a flow with the given name.
	 * 
	 * @param name the name of the flow
	 */
	public SimpleFlow(String name) {
		this.name = name;
	}

	/**
	 * Get the name for this flow.
	 * 
	 * @see Flow#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Public setter for the start state name.
	 * @param startStateName the name of the start state
	 */
	public void setStartStateName(String startStateName) {
		this.startStateName = startStateName;
	}

	/**
	 * Public setter for the stateTransitions.
	 * @param stateTransitions the stateTransitions to set
	 */
	public void setStateTransitions(Collection<StateTransition> stateTransitions) {

		this.stateTransitions = stateTransitions;
	}

	/**
	 * Locate start state and pre-populate data structures needed for execution.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		initializeTransitions();
	}

	/**
	 * @see Flow#start(FlowExecutor)
	 */
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
	public FlowExecution resume(String stateName, FlowExecutor executor) throws FlowExecutionException {

		String status = FlowExecution.UNKNOWN;
		State state = stateMap.get(stateName);

		// Terminate if there are no more states
		while (state != null) {

			stateName = state.getName();

			try {
				status = state.handle(executor);
			}
			catch (Exception e) {
				executor.close(new FlowExecution(stateName, status));
				throw new FlowExecutionException(String.format("Ended flow=%s at state=%s with exception", name,
						stateName), e);
			}

			state = nextState(stateName, status);

		}

		FlowExecution result = new FlowExecution(stateName, status);
		executor.close(result);
		return result; 

	}

	/**
	 * @return the next {@link Step} (or null if this is the end)
	 * @throws JobExecutionException
	 */
	private State nextState(String stateName, String status) throws FlowExecutionException {

		// Special status value indicating that a state wishes to pause
		// execution
		if (status.equals(FlowExecution.PAUSED)) {
			return null;
		}

		Set<StateTransition> set = transitionMap.get(stateName);

		if (set == null) {
			throw new FlowExecutionException(String.format("No transitions found in flow=%s for state=%s", getName(),
					stateName));
		}

		String next = null;
		for (StateTransition stateTransition : set) {
			if (stateTransition.matches(status)) {
				if (stateTransition.isEnd()) {
					// End of job
					return null;
				}
				next = stateTransition.getNext();
				break;
			}
		}

		if (next == null) {
			throw new FlowExecutionException(String.format(
					"Next state not found in flow=%s for state=%s with exit status=%s", getName(), stateName, status));
		}

		if (!stateMap.containsKey(next)) {
			throw new FlowExecutionException(String.format("Next state not specified in flow=%s for next=%s",
					getName(), next));
		}

		return stateMap.get(next);

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

		for (StateTransition stateTransition : stateTransitions) {
			State state = stateTransition.getState();
			stateMap.put(state.getName(), state);
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

			SortedSet<StateTransition> set = transitionMap.get(name);
			if (set == null) {
				set = new TreeSet<StateTransition>();
				transitionMap.put(name, set);
			}
			set.add(stateTransition);

		}

		if (!hasEndStep) {
			throw new IllegalArgumentException(
					"No end state was found.  You must specify at least one transition with no next state.");
		}

		if (startStateName != null) {

			startState = stateMap.get(startStateName);
			if (startState == null) {
				throw new IllegalArgumentException(
						"Start state does not exist (if you specify a startStateName make sure "
								+ "a state with that name is in one of the transitions): [" + startStateName + "]");
			}

		}
		else {

			// Try and locate a transition with no incoming links

			Set<String> nextStateNames = new HashSet<String>();

			for (StateTransition stateTransition : stateTransitions) {
				nextStateNames.add(stateTransition.getNext());
			}

			for (StateTransition stateTransition : stateTransitions) {
				State state = stateTransition.getState();
				if (!nextStateNames.contains(state.getName())) {
					if (startState != null && !startState.getName().equals(state.getName())) {
						throw new IllegalArgumentException(String.format(
								"Multiple possible start states found: [%s, %s].  "
										+ "Please specify one explicitly with the startStateName property.", startState
										.getName(), state.getName()));
					}
					startState = state;
				}
			}

			if (startState == null) {
				throw new IllegalArgumentException(
						"No start state could be located (no transition without incoming links)");
			}

		}
	}

}
