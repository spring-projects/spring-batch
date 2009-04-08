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

package org.springframework.batch.core.partition;

import java.util.Collection;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * Interface defining the responsibilities of controlling the execution of a
 * partitioned {@link StepExecution}. Implementations will need to create a
 * partition with the {@link StepExecutionSplitter}, and then use an execution
 * fabric (grid, etc.), to execute the partitioned step. The results of the
 * executions can be returned raw from remote workers to be aggregated by the
 * caller.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public interface PartitionHandler {

	/**
	 * Main entry point for {@link PartitionHandler} interface. The splitter
	 * creates all the executions that need to be farmed out, along with their
	 * input parameters (in the form of their {@link ExecutionContext}). The
	 * master step execution is used to identify the partition and group
	 * together the results logically.
	 * 
	 * @param stepSplitter a strategy for generating a collection of
	 * {@link StepExecution} instances
	 * @param stepExecution the master step execution for the whole partition
	 * @return a collection of completed {@link StepExecution} instances
	 * @throws Exception if anything goes wrong. This allows implementations to
	 * be liberal and rely on the caller to translate an exception into a step
	 * failure as necessary.
	 */
	Collection<StepExecution> handle(StepExecutionSplitter stepSplitter, StepExecution stepExecution) throws Exception;

}
