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
package org.springframework.batch.core.job.flow;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.flow.Flow;

/**
 * Context and execution strategy for {@link FlowJob} to allow it to delegate
 * its execution step by step.
 * 
 * @author Dave Syer
 * 
 */
public interface JobFlowExecutor {

	/**
	 * @param step a {@link Step} to execute
	 * @return the exit status that drives the surrounding {@link Flow}
	 * @throws StartLimitExceededException
	 * @throws JobRestartException
	 * @throws JobInterruptedException
	 */
	String executeStep(Step step) throws JobInterruptedException, JobRestartException, StartLimitExceededException;

	/**
	 * @return the current {@link JobExecution}
	 */
	JobExecution getJobExecution();

}
