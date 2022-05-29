/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.job.flow.support.state;

import java.util.Collection;

import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;

/**
 * Strategy interface for aggregating {@link FlowExecution} instances into a single exit
 * status.
 *
 * @author Dave Syer
 * @since 2.0
 */
public interface FlowExecutionAggregator {

	/**
	 * @param executions the executions to aggregate
	 * @return a summary status for the whole lot
	 */
	FlowExecutionStatus aggregate(Collection<FlowExecution> executions);

}
