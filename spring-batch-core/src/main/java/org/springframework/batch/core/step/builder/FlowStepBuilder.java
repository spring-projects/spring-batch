/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;

/**
 * @author Dave Syer
 * 
 */
public class FlowStepBuilder extends StepBuilderHelper<FlowStepBuilder> {

	private Flow flow;

	public FlowStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	public FlowStepBuilder flow(Flow flow) {
		this.flow = flow;
		return this;
	}

	public Step build() {
		FlowStep step = new FlowStep();
		step.setName(getName());
		step.setFlow(flow);
		super.enhance(step);
		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return step;
	}

}
