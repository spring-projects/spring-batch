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
import org.springframework.batch.core.job.flow.FlowExecutionStatus;

/**
 * Implementation of the {@link FlowExecutionAggregator} interface that aggregates
 * {@link FlowExecutionStatus}', using the status with the high precedence as the
 * aggregate status.  See {@link FlowExecutionStatus} for details on status
 * precedence.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class MaxValueFlowExecutionAggregator implements FlowExecutionAggregator {

	/**
	 * Aggregate all of the {@link FlowExecutionStatus}es of the
	 * {@link FlowExecution}s into one status. The aggregate status will be the
	 * status with the highest precedence.
	 * 
	 * @see FlowExecutionAggregator#aggregate(Collection)
	 */
	public FlowExecutionStatus aggregate(Collection<FlowExecution> executions) {
		if (executions == null || executions.size() == 0) {
			return FlowExecutionStatus.UNKNOWN;
		}
		return Collections.max(executions).getStatus();
	}

}
