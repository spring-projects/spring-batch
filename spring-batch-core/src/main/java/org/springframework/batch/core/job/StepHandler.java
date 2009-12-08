/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.batch.core.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRestartException;

/**
 * Strategy interface for handling a {@link Step} on behalf of a {@link Job}.
 * 
 * @author Dave Syer
 * 
 */
public interface StepHandler {

	/**
	 * Handle a step and return the execution for it. Does not save the
	 * {@link JobExecution}, but should manage the persistence of the
	 * {@link StepExecution} if required (e.g. at least it needs to be added to
	 * a repository before the step can eb executed).
	 * 
	 * @param step a {@link Step}
	 * @param jobExecution a {@link JobExecution}
	 * @return an execution of the step
	 * 
	 * @throws JobInterruptedException if there is an interruption
	 * @throws JobRestartException if there is a problem restarting a failed
	 * step
	 * @throws StartLimitExceededException if the step exceeds its start limit
	 * 
	 * @see Job#execute(JobExecution)
	 * @see Step#execute(StepExecution)
	 */
	StepExecution handleStep(Step step, JobExecution jobExecution) throws JobInterruptedException, JobRestartException,
			StartLimitExceededException;

}
