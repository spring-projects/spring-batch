/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.core.jsr.job.flow.support;

import java.util.Set;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.xml.SimpleFlowFactoryBean.DelegateState;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionException;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.jsr.job.flow.support.state.JsrStepState;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Implements JSR-352 specific logic around the execution of a flow.  Specifically, this
 * {@link Flow} implementation will attempt to find the next state based on the provided
 * exit status.  If none is found (the exit status isn't mapped), it will attempt to
 * resolve the next state basing it on the last step's batch status.  Only if both
 * attempts fail, the flow will fail due to the inability to find the next state.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class JsrFlow extends SimpleFlow {

	private JsrStepState currentStep;

	/**
	 * @param name name of the flow
	 */
	public JsrFlow(String name) {
		super(name);
	}

	@Nullable
	public String getMostRecentStepName() {
		if(currentStep != null) {
			return currentStep.getStep().getName();
		} else {
			return null;
		}
	}

	@Override
	protected boolean isFlowContinued(State state, FlowExecutionStatus status, StepExecution stepExecution) {
		if(state instanceof DelegateState) {
			state = ((DelegateState) state).getState();
		}

		if(state instanceof JsrStepState) {
			currentStep = (JsrStepState) state;
		}

		return super.isFlowContinued(state, status, stepExecution);
	}

	@Override
	protected State nextState(String stateName, FlowExecutionStatus status, StepExecution stepExecution) throws FlowExecutionException {
		State nextState = findState(stateName, status, stepExecution);

		if(stepExecution != null) {
			ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
			if(executionContext.containsKey("batch.stoppedStep")) {
				String stepName = executionContext.getString("batch.stoppedStep");

				if(stateName.endsWith(stepName)) {
					if(nextState != null && executionContext.containsKey("batch.restartStep") && StringUtils.hasText(executionContext.getString("batch.restartStep"))) {
						nextState = findState(stateName, new FlowExecutionStatus(status.getName() + ".RESTART"), stepExecution);
					}
				}
			}
		}

		return nextState;
	}

	/**
	 * @return the next {@link Step} (or null if this is the end)
	 * @throws FlowExecutionException
	 */
	private State findState(String stateName, FlowExecutionStatus status, StepExecution stepExecution) throws FlowExecutionException {
		Set<StateTransition> set = getTransitionMap().get(stateName);

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
			if(stepExecution != null) {
				exitCode = stepExecution.getStatus().toString();

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
			}

			if(next == null) {
				throw new FlowExecutionException(String.format(
						"Next state not found in flow=%s for state=%s with exit status=%s", getName(), stateName, status.getName()));
			}
		}

		if (!getStateMap().containsKey(next)) {
			throw new FlowExecutionException(String.format("Next state not specified in flow=%s for next=%s",
					getName(), next));
		}

		return getStateMap().get(next);
	}
}
