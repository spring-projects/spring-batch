/*
 * Copyright 2012-2013 the original author or authors.
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
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Dave Syer
 *
 */
public class JobFlowBuilder extends FlowBuilder<FlowJobBuilder> {

	private FlowJobBuilder parent;

	public JobFlowBuilder(FlowJobBuilder parent) {
		super(parent.getName());
		this.parent = parent;
	}

	public JobFlowBuilder(FlowJobBuilder parent, Step step) {
		super(parent.getName());
		this.parent = parent;
		start(step);
	}

	public JobFlowBuilder(FlowJobBuilder parent, JobExecutionDecider decider) {
		super(parent.getName());
		this.parent = parent;
		start(decider);
	}

	public JobFlowBuilder(FlowJobBuilder parent, Flow flow) {
		super(parent.getName());
		this.parent = parent;
		start(flow);
	}

	/**
	 * Build a flow and inject it into the parent builder. The parent builder is then returned so it can be enhanced
	 * before building an actual job.  Normally called explicitly via {@link #end()}.
	 *
	 * @see org.springframework.batch.core.job.builder.FlowBuilder#build()
	 */
	@Override
	public FlowJobBuilder build() {
		Flow flow = flow();

		if(flow instanceof InitializingBean) {
			try {
				((InitializingBean) flow).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new FlowBuilderException(e);
			}
		}

		parent.flow(flow);
		return parent;
	}

}
