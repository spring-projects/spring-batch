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
package org.springframework.batch.core.job.builder;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.step.builder.StepBuilderException;

/**
 * A job builder for {@link FlowJob} instances. A flow job delegates processing to a nested flow composed of steps and
 * conditional transitions between steps.
 * 
 * @author Dave Syer
 * 
 * @since 2.2
 */
public class FlowJobBuilder extends JobBuilderHelper<FlowJobBuilder> {

	private Flow flow;

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common job properties
	 */
	public FlowJobBuilder(JobBuilderHelper<?> parent) {
		super(parent);
	}

	/**
	 * Start a job with this flow, but expect to transition from there to other flows or steps.
	 * 
	 * @param flow the flow to start with
	 * @return a builder to enable fluent chaining
	 */
	public JobFlowBuilder start(Flow flow) {
		return new JobFlowBuilder(this, flow);
	}

	/**
	 * Start a job with this step, but expect to transition from there to other flows or steps.
	 * 
	 * @param step the step to start with
	 * @return a builder to enable fluent chaining
	 */
	public JobFlowBuilder start(Step step) {
		return new JobFlowBuilder(this, step);
	}

	/**
	 * Provide a single flow to execute as the job.
	 * 
	 * @param flow the flow to execute
	 * @return this for fluent chaining
	 */
	protected FlowJobBuilder flow(Flow flow) {
		this.flow = flow;
		return this;
	}

	/**
	 * Build a job that executes the flow provided, normally composed of other steps.
	 * 
	 * @return a flow job
	 */
	public Job build() {
		FlowJob job = new FlowJob();
		job.setName(getName());
		job.setFlow(flow);
		super.enhance(job);
		try {
			job.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}
		return job;
	}

}
