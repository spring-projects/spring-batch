/*
 * Copyright 2006-2008 the original author or authors.
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

import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public interface StepListener {

	/**
	 * Initialise the state of the listener with the {@link StepExecution} from
	 * the current scope.
	 * @param stepExecution
	 */
	void open(StepExecution stepExecution);

	/**
	 * The value returned will be combined with the normal exit status using
	 * {@link ExitStatus#and(ExitStatus)}.
	 * 
	 * @param e an exception thrown by the step execution
	 * @return an exit status to be combined with the normal one, or null
	 */
	ExitStatus onError(Throwable e);

	/**
	 * Give a listener a chance to modify the exit status from a step. The value
	 * returned will be combined with the normal exit status using
	 * {@link ExitStatus#and(ExitStatus)}.
	 * 
	 * @return an {@link ExitStatus} to combine with the normal value. Return
	 * null to leave the old value unchanged.
	 */
	ExitStatus close();
}
