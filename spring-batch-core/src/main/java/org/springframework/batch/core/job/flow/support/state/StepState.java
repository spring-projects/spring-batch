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

package org.springframework.batch.core.job.flow.support.state;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.step.StepHolder;

/**
 * {@link State} implementation that delegates to a {@link FlowExecutor} to
 * execute the specified {@link Step}.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class StepState extends AbstractState implements StepHolder {

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

	/**
	 * @return the step
	 */
	public Step getStep() {
		return step;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.core.job.flow.State#isEndState()
	 */
	public boolean isEndState() {
		return false;
	}

}
