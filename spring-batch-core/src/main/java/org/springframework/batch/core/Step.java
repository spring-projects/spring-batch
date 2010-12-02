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
package org.springframework.batch.core;

/**
 * Batch domain interface representing the configuration of a step. As with the {@link Job}, a {@link Step} is meant to
 * explicitly represent the configuration of a step by a developer, but also the ability to execute the step.
 * 
 * @author Dave Syer
 * 
 */
public interface Step {

	/**
	 * @return the name of this step.
	 */
	String getName();

	/**
	 * @return true if a step that is already marked as complete can be started again.
	 */
	boolean isAllowStartIfComplete();

	/**
	 * @return the number of times a job can be started with the same identifier.
	 */
	int getStartLimit();

	/**
	 * Process the step and assign progress and status meta information to the {@link StepExecution} provided. The
	 * {@link Step} is responsible for setting the meta information and also saving it if required by the
	 * implementation.<br/>
	 * 
	 * It is not safe to re-use an instance of {@link Step} to process multiple concurrent executions.
	 * 
	 * @param stepExecution an entity representing the step to be executed
	 * 
	 * @throws JobInterruptedException if the step is interrupted externally
	 */
	void execute(StepExecution stepExecution) throws JobInterruptedException;

}
