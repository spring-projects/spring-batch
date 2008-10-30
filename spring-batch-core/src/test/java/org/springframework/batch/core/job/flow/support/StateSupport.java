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
package org.springframework.batch.core.job.flow.support;

import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.JobFlowExecutor;
import org.springframework.batch.core.job.flow.support.AbstractState;
import org.springframework.batch.core.job.flow.support.State;

/**
 * Base class for {@link State} implementations.
 * 
 * @author Dave Syer
 *
 */
public class StateSupport extends AbstractState {

	/**
	 * @param name
	 */
	public StateSupport(String name) {
		super(name);
	}

	@Override
	public String handle(JobFlowExecutor executor) throws Exception {
		return FlowExecution.COMPLETED;
	}

}
