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

package org.springframework.batch.core.job;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRestartException;

/**
 * Simple implementation of {@link Job} interface providing the ability to run a
 * {@link JobExecution}. Sequentially executes a job by iterating through its
 * list of steps.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleJob extends AbstractJob {

	private List<Step> steps = new ArrayList<Step>();

	/**
	 * Public setter for the steps in this job. Overrides any calls to
	 * {@link #addStep(Step)}.
	 * 
	 * @param steps the steps to execute
	 */
	public void setSteps(List<Step> steps) {
		this.steps.clear();
		this.steps.addAll(steps);
	}

	/**
	 * Convenience method for adding a single step to the job.
	 * 
	 * @param step a {@link Step} to add
	 */
	public void addStep(Step step) {
		this.steps.add(step);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.batch.core.job.AbstractJob#getStep(java.lang.String)
	 */
	public Step getStep(String stepName){
		for (Step step : this.steps) {
			if(step.getName().equals(stepName))
			{
				return step;
			}
		}
		return null;
	}
	
	/**
	 * Handler of steps sequentially as provided, checking each one for success
	 * before moving to the next. Returns the last {@link StepExecution}
	 * successfully processed if it exists, and null if none were processed.
	 * 
	 * @param execution the current {@link JobExecution}
	 * @return the last successful {@link StepExecution}
	 * 
	 * @see AbstractJob#handleStep(Step, JobExecution)
	 */
	protected StepExecution doExecute(JobExecution execution) throws JobInterruptedException, JobRestartException,
			StartLimitExceededException {

		StepExecution stepExecution = null;
		for (Step step : steps) {
			stepExecution = handleStep(step, execution);
			if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
				return stepExecution;
			}
		}

		return stepExecution;
	}

}
