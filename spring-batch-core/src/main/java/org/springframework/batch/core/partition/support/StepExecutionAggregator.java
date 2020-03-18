/*
 * Copyright 2008-2009 the original author or authors.
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
package org.springframework.batch.core.partition.support;

import java.util.Collection;

import org.springframework.batch.core.StepExecution;

/**
 * Strategy for a aggregating step executions, usually when they are the result
 * of partitioned or remote execution.
 * 
 * @author Dave Syer
 * 
 * @since 2.1
 * 
 */
public interface StepExecutionAggregator {

	/**
	 * Take the inputs and aggregate, putting the aggregates into the result.
	 * 
	 * @param result the result to overwrite
	 * @param executions the inputs
	 */
	void aggregate(StepExecution result, Collection<StepExecution> executions);

}