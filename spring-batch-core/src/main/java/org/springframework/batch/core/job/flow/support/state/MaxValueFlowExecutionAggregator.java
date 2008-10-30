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

import org.springframework.batch.core.job.flow.FlowExecution;

/**
 * @author Dave Syer
 *
 */
public class MaxValueFlowExecutionAggregator implements FlowExecutionAggregator {

	/**
	 * @see FlowExecutionAggregator#aggregate(Collection)
	 */
	public String aggregate(Collection<FlowExecution> executions) {
		if (executions==null || executions.size()==0) {
			return FlowExecution.UNKNOWN;
		}
		return Collections.max(executions).getStatus();
	}

}
