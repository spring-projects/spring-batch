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
package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.support.state.AbstractState;

/**
 * Base class for {@link State} implementations in test cases.
 * 
 * @author Dave Syer
 *
 */
public class StateSupport extends AbstractState {

	private FlowExecutionStatus status;
	
	public StateSupport(String name) {
		this(name, FlowExecutionStatus.COMPLETED);
	}

	public StateSupport(String name, FlowExecutionStatus status) {
		super(name);
		this.status = status;
	}

	@Override
	public FlowExecutionStatus handle(FlowExecutor executor) throws Exception {
		JobExecution jobExecution = executor.getJobExecution();
		if (jobExecution != null) {
			jobExecution.createStepExecution(getName());
		}
		return this.status;
	}
	
	public boolean isEndState() {
		return false;
	}

}
