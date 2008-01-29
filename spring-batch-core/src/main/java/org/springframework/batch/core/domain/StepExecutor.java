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

package org.springframework.batch.core.domain;

import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Interface for processing a step. Implementations are free to process the step
 * and return when finished, or to schedule the step for processing
 * concurrently, or in the future. The status of the execution should be
 * trackable with the step execution. The step should be treated as immutable.<br/>
 * 
 * Because step execution parameters and policies can vary from step to step, a
 * {@link StepExecutor} should be created by the caller using a {@link Step}.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public interface StepExecutor {

	/**
	 * It is not safe to re-use an instance of {@link StepExecutor} to process
	 * multiple concurrent executions. Use the factory method in {@link Step} to
	 * create a new instance and execute that.
	 * 
	 * @param stepExecution an entity representing the step to be executed
	 * 
	 * @throws StepInterruptedException if the step is interrupted externally
	 * @throws BatchCriticalException if there is a problem that needs to be
	 * signalled to the caller
	 */
	ExitStatus process(StepExecution stepExecution) throws StepInterruptedException, BatchCriticalException;

}
