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
package org.springframework.batch.core.configuration;

import org.springframework.batch.core.tasklet.Tasklet;

/**
 * Batch domain interface representing the configuration of a step. As with the
 * (@link JobConfiguration), step configuration is meant to explicitly represent
 * a the configuration of a step by a developer. This allows for the separation
 * of what a developer configures from the myriad of concerns required for
 * executing a job.
 * 
 * @author Dave Syer
 * 
 */
public interface StepConfiguration {

	/**
	 * @return the name of this step configuration.
	 */
	String getName();

	/**
	 * @return the {@link Tasklet} instance to execute for each item processed.
	 */
	Tasklet getTasklet();

	/**
	 * @return true if a step that is already marked as complete can be started
	 * again.
	 */
	boolean isAllowStartIfComplete();
	
	/**
	 * Flag to indicate if restart data needs to be saved for this step.
	 * @return true if restart data should be saved
	 */
	boolean isSaveRestartData();

	/**
	 * @return the number of times a job can be started with the same
	 * identifier.
	 */
	int getStartLimit();

}