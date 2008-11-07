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
package org.springframework.batch.core.step.tasklet;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.core.AttributeAccessor;

/**
 * Strategy for processing in a step.
 * 
 * @author Dave Syer
 * 
 */
public interface Tasklet {

	/**
	 * Given the current context in the form of a step contribution, do whatever
	 * is necessary to process this unit inside a transaction. Implementations
	 * return {@link ExitStatus#FINISHED} if finished. If not they return
	 * {@link ExitStatus#CONTINUABLE}. On failure throws an exception.
	 * 
	 * @param contribution mutable state to be passed back to update the current
	 * step execution
	 * @param attributes attributes shared between invocations but not between
	 * restarts
	 * @return an {@link ExitStatus} indicating whether processing is
	 * continuable.
	 */
	ExitStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception;

}
