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

import org.springframework.batch.core.domain.StepExecution;

/**
 * Policy for determining whether or not reading should continue.
 * 
 * @author Lucas Ward
 */
public interface ReadFailurePolicy {

	/**
	 * Returns true or false, indicating whether or not reading should
	 * continue for the current step execution with the given throwable.
	 * 
	 * @param ex throwable encountered while reading
	 * @param stepExecution currently running execution
	 * @return true if reading should continue, false otherwise.
	 * @throws IllegalArgumentException if t or stepExecution is null
	 */
	boolean shouldContinue(Exception ex, StepExecution stepExecution);
}
