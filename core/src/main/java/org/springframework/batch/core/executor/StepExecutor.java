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

package org.springframework.batch.core.executor;

import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Interface for processing a step. Implementations are free to process the step
 * and return when finished, or to schedule the step for processing
 * concurrently, or in the future. The status of the execution should be
 * trackable with the step execution context ({@see Step#getContext()}). The
 * configuration should be treated as immutable.<br/>
 * 
 * Because step execution parameters and policies can vary from step to step, a
 * {@link StepExecutor} should be created by the caller using a
 * {@link StepExecutorFactory}.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public interface StepExecutor {

	/**
	 * Process the step according to the given configuration.
	 * 
	 * @param configuration the configuration to use when running the step.
	 * Contains a recipe for the business logic of an individual processing
	 * operation. Also used to determine policies for commit intervals and
	 * exception handling, for instance.
	 * @param stepExecution an entity representing the step to be executed
	 * @throws StepInterruptedException if the step is interrupted externally
	 * @throws BatchCriticalException if there is a problem that needs to be
	 * signalled to the caller
	 */
	ExitStatus process(StepConfiguration configuration, StepExecution stepExecution) throws StepInterruptedException, BatchCriticalException;

}
