/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.jsr.job.flow.support.state;

import java.util.Collections;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;

/**
 * Extends {@link org.springframework.batch.core.job.flow.support.state.StepState} to persist what the
 * last step that was executed was (used in Decisions and restarts).
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrStepState extends org.springframework.batch.core.job.flow.support.state.StepState {

	/**
	 * @param step the step that will be executed
	 */
	public JsrStepState(Step step) {
		super(step);
	}

	/**
	 * @param name for the step that will be executed
	 * @param step the step that will be executed
	 */
	public JsrStepState(String name, Step step) {
		super(name, step);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.job.flow.support.state.StepState#handle(org.springframework.batch.core.job.flow.FlowExecutor)
	 */
	@Override
	public FlowExecutionStatus handle(FlowExecutor executor) throws Exception {
		FlowExecutionStatus result = super.handle(executor);

		executor.getJobExecution().getExecutionContext().put("batch.lastSteps", Collections.singletonList(getStep().getName()));

		return result;
	}
}
