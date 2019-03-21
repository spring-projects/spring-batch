/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;

/**
 * A step builder for {@link FlowStep} instances. A flow step delegates processing to a nested flow composed of other
 * steps.
 * 
 * @author Dave Syer
 * 
 * @since 2.2
 */
public class FlowStepBuilder extends StepBuilderHelper<FlowStepBuilder> {

	private Flow flow;

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common step properties
	 */
	public FlowStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	/**
	 * Provide a flow to execute during the step.
	 * 
	 * @param flow the flow to execute
	 * @return this for fluent chaining
	 */
	public FlowStepBuilder flow(Flow flow) {
		this.flow = flow;
		return this;
	}

	/**
	 * Build a step that executes the flow provided, normally composed of other steps. The flow is not executed in a
	 * transaction because the individual steps are supposed to manage their own transaction state.
	 * 
	 * @return a flow step
	 */
	public Step build() {
		FlowStep step = new FlowStep();
		step.setName(getName());
		step.setFlow(flow);
		super.enhance(step);
		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}
		return step;
	}

}
