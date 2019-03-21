/*
 * Copyright 2006-2013 the original author or authors.
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

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;

/**
 * Convenience for building jobs of various kinds.
 *
 * @author Dave Syer
 *
 * @since 2.2
 *
 */
public class JobBuilder extends JobBuilderHelper<JobBuilder> {

	/**
	 * Create a new builder for a job with the given name.
	 *
	 * @param name the name of the job
	 */
	public JobBuilder(String name) {
		super(name);
	}

	/**
	 * Create a new job builder that will execute a step or sequence of steps.
	 *
	 * @param step a step to execute
	 * @return a {@link SimpleJobBuilder}
	 */
	public SimpleJobBuilder start(Step step) {
		return new SimpleJobBuilder(this).start(step);
	}

	/**
	 * Create a new job builder that will execute a flow.
	 *
	 * @param flow a flow to execute
	 * @return a {@link SimpleJobBuilder}
	 */
	public JobFlowBuilder start(Flow flow) {
		return new FlowJobBuilder(this).start(flow);
	}

	/**
	 * Create a new job builder that will execute a step or sequence of steps.
	 *
	 * @param step a step to execute
	 * @return a {@link SimpleJobBuilder}
	 */
	public JobFlowBuilder flow(Step step) {
		return new FlowJobBuilder(this).start(step);
	}
}
