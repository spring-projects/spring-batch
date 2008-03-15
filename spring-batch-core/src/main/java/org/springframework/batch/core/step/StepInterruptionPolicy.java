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

package org.springframework.batch.core.step;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

/**
 * Strategy interface for an interruption policy. This policy allows
 * {@link Step} implementations to check if a job has been interrupted.
 * 
 * @author Lucas Ward
 * 
 */
public interface StepInterruptionPolicy {

	/**
	 * Has the job been interrupted? If so then throw a
	 * {@link JobInterruptedException}.
	 * @param stepExecution the current context of the running step.
	 * 
	 * @throws JobInterruptedException when the job has been interrupted.
	 */
	void checkInterrupted(StepExecution stepExecution) throws JobInterruptedException;
}
