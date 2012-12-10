/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.DecisionState;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.FlowState;
import org.springframework.batch.core.job.flow.support.state.SplitState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.core.task.TaskExecutor;

/**
 * A builder for a flow of steps that can be executed as a job or as part of a job. Steps can be linked together with
 * conditional transitions that depend on the exit status of the previous step.
 * 
 * @author Dave Syer
 * 
 * @since 2.2
 * 
 * @param <Q> the type of object returned by the builder (by default a Flow)
 * 
 */
public class FlowBuilder<Q> {

	private String name;

	private String prefix;

	private List<StateTransition> transitions = new ArrayList<StateTransition>();

	private Map<String, State> tos = new HashMap<String, State>();

	private State currentState;

	private EndState failedState;

	private EndState completedState;

	private int decisionCounter = 0;

	private int splitCounter = 0;

	private Map<Object, State> states = new HashMap<Object, State>();

	private SimpleFlow flow;

	private boolean dirty = true;

	public FlowBuilder(String name) {
		this.name = name;
		this.prefix = name + ".";
		this.failedState = new EndState(FlowExecutionStatus.FAILED, prefix + "FAILED");
		this.completedState = new EndState(FlowExecutionStatus.COMPLETED, prefix + "COMPLETED");
	}

	/**
	 * Validate the current state of the builder and build a flow. Subclasses may override this to build an object of a
	 * different type that itself depends on the flow.
	 * 
	 * @return a flow
	 */
	public Q build() {
		@SuppressWarnings("unchecked")
		Q result = (Q) flow();
		return result;
	}

	/**
	 * Transition to the next step on successful completion of the current step. All other outcomes are treated as
	 * failures.
	 * 
	 * @param step the next step
	 * @return this to enable chaining
	 */
	public FlowBuilder<Q> next(Step step) {
		doNext(step);
		return this;
	}

	/**
	 * Start a flow. If some steps are already registered, just a synonym for {@link #from(Step)}.
	 * 
	 * @param step the step to start with
	 * @return this to enable chaining
	 */
	public FlowBuilder<Q> start(Step step) {
		doStart(step);
		return this;
	}

	/**
	 * Go back to a previously registered step and start a new path. If no steps are registered yet just a synonym for
	 * {@link #start(Step)}.
	 * 
	 * @param step the step to start from (already registered)
	 * @return this to enable chaining
	 */
	public FlowBuilder<Q> from(Step step) {
		doFrom(step);
		return this;
	}

	/**
	 * Transition to the decider on successful completion of the current step. All other outcomes are treated as
	 * failures.
	 * 
	 * @param step the next step
	 * @return this to enable chaining
	 */
	public UnterminatedFlowBuilder<Q> next(JobExecutionDecider decider) {
		doNext(decider);
		return new UnterminatedFlowBuilder<Q>(this);
	}

	/**
	 * If a flow should start with a decision use this as the first state.
	 * 
	 * @param decider the to start from
	 * @return a builder to enable chaining
	 */
	public UnterminatedFlowBuilder<Q> start(JobExecutionDecider decider) {
		doStart(decider);
		return new UnterminatedFlowBuilder<Q>(this);
	}

	/**
	 * Start again from a decision that was already registered.
	 * 
	 * @param decider the decider to start from (already registered)
	 * @return a builder to enable chaining
	 */
	public UnterminatedFlowBuilder<Q> from(JobExecutionDecider decider) {
		doFrom(decider);
		return new UnterminatedFlowBuilder<Q>(this);
	}

	/**
	 * Go next on successful completion to a subflow.
	 * 
	 * @param flow the flow to go to
	 * @return a builder to enable chaining
	 */
	public FlowBuilder<Q> next(Flow flow) {
		doNext(flow);
		return this;
	}

	/**
	 * Start again from a subflow that was already registered.
	 * 
	 * @param flow the flow to start from (already registered)
	 * @return a builder to enable chaining
	 */
	public FlowBuilder<Q> from(Flow flow) {
		doFrom(flow);
		return this;
	}

	/**
	 * If a flow should start with a subflow use this as the first state.
	 * 
	 * @param flow the flow to start from
	 * @return a builder to enable chaining
	 */
	public FlowBuilder<Q> start(Flow flow) {
		doStart(flow);
		return this;
	}

	/**
	 * @param executor a task executor to execute the split flows
	 * @return a builder to enable fluent chaining
	 */
	public SplitBuilder<Q> split(TaskExecutor executor) {
		return new SplitBuilder<Q>(this, executor);
	}

	/**
	 * Start a transition to a new state if the exit status from the previous state matches the pattern given.
	 * Successful completion normally results in an exit status equal to (or starting with by convention) "COMPLETED".
	 * See {@link ExitStatus} for commonly used values.
	 * 
	 * @param pattern the pattern of exit status on which to take this transition
	 * @return a builder to enable fluent chaining
	 */
	public TransitionBuilder<Q> on(String pattern) {
		return new TransitionBuilder<Q>(this, pattern);
	}

	/**
	 * A synonym for {@link #build()} which callers might find useful. Subclasses can override build to create an object
	 * of the desired type (e.g. a parent builder or an actual flow).
	 * 
	 * @return the result of the builder
	 * 
	 * @param Q the type of the result
	 */
	public final Q end() {
		return build();
	}

	protected Flow flow() {
		if (!dirty) {
			// optimization in case this method is called consecutively
			return flow;
		}
		flow = new SimpleFlow(name);
		// optimization for flows that only have one state that itself is a flow:
		if (currentState instanceof FlowState && states.size() == 1) {
			return ((FlowState) currentState).getFlows().iterator().next();
		}
		addDanglingEndStates();
		flow.setStateTransitions(transitions);
		dirty = false;
		return flow;
	}

	private void doNext(Object input) {
		if (this.currentState == null) {
			doStart(input);
		}
		State next = createState(input);
		addTransition("COMPLETED", next);
		addTransition("*", failedState);
		this.currentState = next;
	}

	private void doStart(Object input) {
		if (this.currentState != null) {
			doFrom(input);
		}
		this.currentState = createState(input);
	}

	private void doFrom(Object input) {
		if (currentState == null) {
			doStart(input);
		}
		State state = createState(input);
		tos.put(currentState.getName(), currentState);
		this.currentState = state;
	}

	private State createState(Object input) {
		State result;
		if (input instanceof Step) {
			if (!states.containsKey(input)) {
				Step step = (Step) input;
				states.put(input, new StepState(prefix + step.getName(), step));
			}
			result = states.get(input);
		}
		else if (input instanceof JobExecutionDecider) {
			if (!states.containsKey(input)) {
				states.put(input, new DecisionState((JobExecutionDecider) input, prefix + "decision"
						+ (decisionCounter++)));
			}
			result = states.get(input);
		}
		else if (input instanceof Flow) {
			if (!states.containsKey(input)) {
				states.put(input, new FlowState((Flow) input, prefix + ((Flow) input).getName()));
			}
			result = states.get(input);
		}
		else {
			throw new FlowBuilderException("No state can be created for: " + input);
		}
		dirty = true;
		return result;
	}

	private SplitState createState(Collection<Flow> flows, TaskExecutor executor) {
		if (!states.containsKey(flows)) {
			states.put(flows, new SplitState(flows, prefix + "split" + (splitCounter++)));
		}
		SplitState result = (SplitState) states.get(flows);
		if (executor != null) {
			result.setTaskExecutor(executor);
		}
		dirty = true;
		return result;
	}

	private void addDanglingEndStates() {
		Set<String> froms = new HashSet<String>();
		for (StateTransition transition : transitions) {
			froms.add(transition.getState().getName());
		}
		if (tos.isEmpty() && currentState != null) {
			tos.put(currentState.getName(), currentState);
		}
		Map<String, State> copy = new HashMap<String, State>(tos);
		// Find all the states that are really end states but not explicitly declared as such
		for (String to : copy.keySet()) {
			if (!froms.contains(to)) {
				currentState = copy.get(to);
				if (currentState != completedState) {
					addTransition("COMPLETED", completedState);
				}
				if (currentState != failedState) {
					addTransition("*", failedState);
				}
			}
		}
		copy = new HashMap<String, State>(tos);
		// Then find the states that do not have a default transition
		for (String from : copy.keySet()) {
			currentState = copy.get(from);
			if (currentState != failedState) {
				if (!hasFail(from)) {
					addTransition("*", failedState);
				}
			}
			if (currentState != completedState) {
				if (!hasCompleted(from)) {
					addTransition("*", completedState);
				}
			}
		}
	}

	private boolean hasFail(String from) {
		return matches(from, "FAILED");
	}

	private boolean hasCompleted(String from) {
		return matches(from, "COMPLETED");
	}

	private boolean matches(String from, String status) {
		for (StateTransition transition : transitions) {
			if (from.equals(transition.getState().getName()) && transition.matches(status)) {
				return true;
			}
		}
		return false;
	}

	private void addTransition(String pattern, State next) {
		tos.put(next.getName(), next);
		transitions.add(StateTransition.createStateTransition(currentState, pattern, next.getName()));
		if (transitions.size() == 1) {
			transitions.add(StateTransition.createEndStateTransition(failedState));
			transitions.add(StateTransition.createEndStateTransition(completedState));
		}
		dirty = true;
	}

	private void end(String pattern) {
		addTransition(pattern, completedState);
	}

	private void fail(String pattern) {
		addTransition(pattern, failedState);
	}

	/**
	 * A builder for continuing a flow from a decision state.
	 * 
	 * @author Dave Syer
	 * 
	 * @param <Q> the result of the builder's build()
	 */
	public static class UnterminatedFlowBuilder<Q> {

		private final FlowBuilder<Q> parent;

		public UnterminatedFlowBuilder(FlowBuilder<Q> parent) {
			this.parent = parent;
		}

		/**
		 * Start a transition to a new state if the exit status from the previous state matches the pattern given.
		 * Successful completion normally results in an exit status equal to (or starting with by convention)
		 * "COMPLETED". See {@link ExitStatus} for commonly used values.
		 * 
		 * @param pattern the pattern of exit status on which to take this transition
		 * @return
		 */
		public TransitionBuilder<Q> on(String pattern) {
			return new TransitionBuilder<Q>(parent, pattern);
		}

	}

	/**
	 * A builder for transitions within a flow.
	 * 
	 * @author Dave Syer
	 * 
	 * @param <Q> the result of the parent builder's build()
	 */
	public static class TransitionBuilder<Q> {

		private final FlowBuilder<Q> parent;

		private final String pattern;

		public TransitionBuilder(FlowBuilder<Q> parent, String pattern) {
			this.parent = parent;
			this.pattern = pattern;
		}

		/**
		 * Specify the next step.
		 * 
		 * @param step the next step after this transition
		 * @return a FlowBuilder
		 */
		public FlowBuilder<Q> to(Step step) {
			State next = parent.createState(step);
			parent.addTransition(pattern, next);
			parent.currentState = next;
			return parent;
		}

		/**
		 * Specify the next state as a complete flow.
		 * 
		 * @param step the next step after this transition
		 * @return a FlowBuilder
		 */
		public FlowBuilder<Q> to(Flow flow) {
			State next = parent.createState(flow);
			parent.addTransition(pattern, next);
			parent.currentState = next;
			return parent;
		}

		/**
		 * Specify the next state as a decision.
		 * 
		 * @param step the next step after this transition
		 * @return a FlowBuilder
		 */
		public FlowBuilder<Q> to(JobExecutionDecider decider) {
			State next = parent.createState(decider);
			parent.addTransition(pattern, next);
			parent.currentState = next;
			return parent;
		}

		/**
		 * Signal the successful end of the flow.
		 * 
		 * @return a FlowBuilder
		 */
		public FlowBuilder<Q> end() {
			parent.end(pattern);
			return parent;
		}

		/**
		 * Signal the end of the flow with an error condition.
		 * 
		 * @return a FlowBuilder
		 */
		public FlowBuilder<Q> fail() {
			parent.fail(pattern);
			return parent;
		}
	}

	/**
	 * A builder for building a split state. Example (<code>builder</code> is a {@link FlowBuilder}):
	 * 
	 * <pre>
	 * Flow splitFlow = builder.start(flow1).split(new SyncTaskExecutor()).add(flow2).build();
	 * </pre>
	 * 
	 * where <code>flow1</code> and <code>flow2</code> will be executed (one after the other because of the task
	 * executor that was added). Another example
	 * 
	 * <pre>
	 * Flow splitFlow = builder.start(step1).split(new SimpleAsyncTaskExecutor()).add(flow).build();
	 * </pre>
	 * 
	 * In this example, a flow consisting of <code>step1</code> will be executed in parallel with <code>flow</code>.
	 * 
	 * @author Dave Syer
	 * 
	 * @param <Q> the result of the parent builder's build()
	 */
	public static class SplitBuilder<Q> {

		private final FlowBuilder<Q> parent;

		private TaskExecutor executor;

		/**
		 * @param parent the parent builder
		 * @param executor the task executor to use in the split
		 */
		public SplitBuilder(FlowBuilder<Q> parent, TaskExecutor executor) {
			this.parent = parent;
			this.executor = executor;
		}

		/**
		 * Add flows to the split, in addition to the current state already present in the parent builder.
		 * 
		 * @param flows more flows to add to the split
		 * @return the parent builder
		 */
		public FlowBuilder<Q> add(Flow... flows) {
			Collection<Flow> list = new ArrayList<Flow>(Arrays.asList(flows));
			String name = "split" + (parent.splitCounter++);
			int counter = 0;
			State one = parent.currentState;
			Flow flow = null;
			if (!(one instanceof FlowState)) {
				FlowBuilder<Flow> stateBuilder = new FlowBuilder<Flow>(name + "_" + (counter++));
				stateBuilder.currentState = one;
				flow = stateBuilder.build();
			}
			if (flow != null) {
				list.add(flow);
			}
			State next = parent.createState(list, executor);
			parent.currentState = next;
			return parent;
		}

	}

}
