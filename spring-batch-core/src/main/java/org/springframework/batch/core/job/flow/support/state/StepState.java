/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.core.job.flow.support.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepHolder;
import org.springframework.batch.core.step.StepLocator;

/**
 * {@link State} implementation that delegates to a {@link FlowExecutor} to
 * execute the specified {@link Step}.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class StepState extends AbstractState implements StepLocator, StepHolder {

	private final Step step;

	/**
	 * @param step the step that will be executed
	 */
	public StepState(Step step) {
		super(step.getName());
		this.step = step;
	}

	/**
	 * @param name for the step that will be executed
	 * @param step the step that will be executed
	 */
	public StepState(String name, Step step) {
		super(name);
		this.step = step;
	}

	@Override
	public FlowExecutionStatus handle(FlowExecutor executor) throws Exception {
		/*
		 * On starting a new step, possibly upgrade the last execution to make
		 * sure it is abandoned on restart if it failed.
		 */
		executor.abandonStepExecution();
		return new FlowExecutionStatus(executor.executeStep(step));
	}

	@Override
	public Step getStep() {
		return step;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.job.flow.State#isEndState()
	 */
	@Override
	public boolean isEndState() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.step.StepLocator#getStepNames()
	 */
	@Override
	public Collection<String> getStepNames() {
		List<String> names = new ArrayList<>();

		names.add(step.getName());

		if(step instanceof StepLocator) {
			names.addAll(((StepLocator)step).getStepNames());
		}

		return names;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.step.StepLocator#getStep(java.lang.String)
	 */
	@Override
	public Step getStep(String stepName) throws NoSuchStepException {
		Step result = null;

		if(step.getName().equals(stepName)) {
			result = step;
		} else if(step instanceof StepLocator) {
			result = ((StepLocator) step).getStep(stepName);
		}

		return result;
	}
}
