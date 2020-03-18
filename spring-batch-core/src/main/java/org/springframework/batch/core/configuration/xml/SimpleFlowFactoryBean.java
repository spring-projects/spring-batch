/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.FlowHolder;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.AbstractState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Convenience factory for SimpleFlow instances for use in XML namespace. It
 * replaces the states in the input with proxies that have a unique name formed
 * from the flow name and the original state name (unless the name is already in
 * that form, in which case it is not modified).
 *
 * @author Dave Syer
 * @author Michael Minella
 */
public class SimpleFlowFactoryBean implements FactoryBean<SimpleFlow>, InitializingBean {

	private String name;

	private List<StateTransition> stateTransitions;

	private String prefix;

	private Comparator<StateTransition> stateTransitionComparator;

	private Class<SimpleFlow> flowType;

	/**
	 * @param stateTransitionComparator {@link Comparator} implementation that addresses
	 * the ordering of state evaluation
	 */
	public void setStateTransitionComparator(Comparator<StateTransition> stateTransitionComparator) {
		this.stateTransitionComparator = stateTransitionComparator;
	}

	/**
	 * @param flowType Used to inject the type of flow (regular Spring Batch or JSR-352)
	 */
	public void setFlowType(Class<SimpleFlow> flowType) {
		this.flowType = flowType;
	}

	/**
	 * The name of the flow that is created by this factory.
	 *
	 * @param name the value of the name
	 */
	public void setName(String name) {
		this.name = name;
		this.prefix = name + ".";
	}

	/**
	 * The raw state transitions for the flow. They will be transformed into
	 * proxies that have the same behavior but unique names prefixed with the
	 * flow name.
	 *
	 * @param stateTransitions the list of transitions
	 */
	public void setStateTransitions(List<StateTransition> stateTransitions) {
		this.stateTransitions = stateTransitions;
	}

	/**
	 * Check mandatory properties (name).
	 *
	 * @throws Exception thrown if error occurs.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(name, "The flow must have a name");

		if(flowType == null) {
			flowType = SimpleFlow.class;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public SimpleFlow getObject() throws Exception {
		SimpleFlow flow = flowType.getConstructor(String.class).newInstance(name);

		flow.setStateTransitionComparator(stateTransitionComparator);

		List<StateTransition> updatedTransitions = new ArrayList<>();
		for (StateTransition stateTransition : stateTransitions) {
			State state = getProxyState(stateTransition.getState());
			updatedTransitions.add(StateTransition.switchOriginAndDestination(stateTransition, state,
					getNext(stateTransition.getNext())));
		}

		flow.setStateTransitions(updatedTransitions);
		flow.afterPropertiesSet();
		return flow;

	}

	private String getNext(String next) {
		if (next == null) {
			return null;
		}
		return (next.startsWith(this.prefix) ? "" : this.prefix) + next;
	}

	/**
	 * Convenience method to get a state that proxies the input but with a
	 * different name, appropriate to this flow. If the state is a StepState
	 * then the step name is also changed.
	 *
	 * @param state
	 * @return
	 */
	private State getProxyState(State state) {
		String oldName = state.getName();
		if (oldName.startsWith(prefix)) {
			return state;
		}
		String stateName = prefix + oldName;
		if (state instanceof StepState) {
			return createNewStepState(state, oldName, stateName);
		}
		return new DelegateState(stateName, state);
	}

	/**
	 * Provides an extension point to provide alternative {@link StepState}
	 * implementations within a {@link SimpleFlow}
	 *
	 * @param state The state that will be used to create the StepState
	 * @param oldName The name to be replaced
	 * @param stateName The name for the new State
	 * @return a state for the requested data
	 */
	protected State createNewStepState(State state, String oldName,
			String stateName) {
		return new StepState(stateName, ((StepState) state).getStep(oldName));
	}

	@Override
	public Class<?> getObjectType() {
		return SimpleFlow.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * A State that proxies a delegate and changes its name but leaves its
	 * behavior unchanged.
	 *
	 * @author Dave Syer
	 *
	 */
	public static class DelegateState extends AbstractState implements FlowHolder {
		private final State state;

		private DelegateState(String name, State state) {
			super(name);
			this.state = state;
		}

		public State getState() {
			return this.state;
		}

		@Override
		public boolean isEndState() {
			return state.isEndState();
		}

		@Override
		public FlowExecutionStatus handle(FlowExecutor executor) throws Exception {
			return state.handle(executor);
		}

		@Override
		public Collection<Flow> getFlows() {
			return (state instanceof FlowHolder) ? ((FlowHolder)state).getFlows() : Collections.<Flow>emptyList();
		}

	}

}
