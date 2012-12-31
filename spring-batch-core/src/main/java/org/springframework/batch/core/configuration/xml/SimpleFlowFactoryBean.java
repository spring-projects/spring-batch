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
package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 *
 */
@SuppressWarnings("rawtypes")
public class SimpleFlowFactoryBean implements FactoryBean, InitializingBean {

	private String name;

	private List<StateTransition> stateTransitions;

	private String prefix;

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
	 * @throws Exception
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(name, "The flow must have a name");
	}

	@Override
	public Object getObject() throws Exception {

		SimpleFlow flow = new SimpleFlow(name);

		List<StateTransition> updatedTransitions = new ArrayList<StateTransition>();
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
			return new StepState(stateName, ((StepState) state).getStep());
		}
		return new DelegateState(stateName, state);
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
	private static class DelegateState extends AbstractState implements FlowHolder {
		private final State state;

		private DelegateState(String name, State state) {
			super(name);
			this.state = state;
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
