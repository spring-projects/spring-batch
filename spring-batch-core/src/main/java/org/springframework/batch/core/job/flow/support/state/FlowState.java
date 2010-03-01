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

import java.util.Collection;
import java.util.Collections;

import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.FlowHolder;

/**
 * State that delegates to a Flow
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class FlowState extends AbstractState implements FlowHolder {

	private final Flow flow;

	/**
	 * @param name
	 */
	public FlowState(Flow flow, String name) {
		super(name);
		this.flow = flow;
	}
	
	/**
	 * @return the flows
	 */
	public Collection<Flow> getFlows() {
		return Collections.singleton(flow);
	}

	@Override
	public FlowExecutionStatus handle(FlowExecutor executor) throws Exception {
		return flow.start(executor).getStatus();
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.core.job.flow.State#isEndState()
	 */
	public boolean isEndState() {
		return false;
	}

}