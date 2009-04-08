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

import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;

/**
 * Strategy interface for generating input contexts for a partitioned step
 * execution independent from the fabric they are going to run on.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public interface StepExecutionSplitter {

	/**
	 * The name of the step configuration that will be executed remotely. Remote
	 * workers are going to execute a the same step for each execution context
	 * in the partition.
	 * @return the name of the step that will execute the business logic
	 */
	String getStepName();

	/**
	 * Partition the provided {@link StepExecution} into a set of parallel
	 * executable instances with the same parent {@link JobExecution}. The grid
	 * size will be treated as a hint for the size of the collection to be
	 * returned. It may or may not correspond to the physical size of an
	 * execution grid.<br/>
	 * <br/>
	 * 
	 * On a restart clients of the {@link StepExecutionSplitter} should expect
	 * it to reconstitute the state of the last failed execution and only return
	 * those executions that need to be restarted. Thus the grid size hint will
	 * be ignored on a restart.
	 * 
	 * @param stepExecution the {@link StepExecution} to be partitioned.
	 * @param gridSize a hint for the splitter if the size of the grid is known
	 * @return a set of {@link StepExecution} instances for remote processing
	 * 
	 * @throws JobExecutionException if the split cannot be made
	 */
	Set<StepExecution> split(StepExecution stepExecution, int gridSize) throws JobExecutionException;

}