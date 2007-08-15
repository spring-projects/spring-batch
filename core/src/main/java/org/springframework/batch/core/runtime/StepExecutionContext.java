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
package org.springframework.batch.core.runtime;

import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.util.Assert;

/**
 * Context for an executing step within a job. Maintains invariants and provides
 * communication channel for all components requiring information about the
 * step.
 * 
 * @author Dave Syer
 * 
 */
public class StepExecutionContext {

	private JobExecutionContext jobExecutionContext;

	private final StepInstance step;

	private final StepExecution stepExecution;

	/**
	 * Constructor with all the mandatory properties.
	 * 
	 * @param jobExecutionContext
	 */
	public StepExecutionContext(JobExecutionContext jobExecutionContext, StepInstance step) {
		super();
		Assert.notNull(jobExecutionContext);
		Assert.notNull(jobExecutionContext.getJobExecution(), "The JobExecutionContext must have a JobExecution");
		Assert.notNull(step);
		this.jobExecutionContext = jobExecutionContext;
		this.step = step;
		this.stepExecution = new StepExecution(step.getId(), jobExecutionContext.getJobExecution().getId());
	}

	/**
	 * Accessor for the step governing this execution.
	 * @return the step
	 */
	public StepInstance getStep() {
		return step;
	}

	/**
	 * Accessor for the execution context information of the enclosing job.
	 * @return the {@link jobExecutionContext} that was used to start this step
	 * execution.
	 */
	public JobExecutionContext getJobExecutionContext() {
		return jobExecutionContext;
	}

	/**
	 * Retrieve the current step execution or create a new one if there is none.
	 * @return the current step execution.
	 */
	public StepExecution getStepExecution() {
		return stepExecution;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof StepExecutionContext)) {
			return super.equals(obj);
		}
		StepExecutionContext other = (StepExecutionContext) obj;
		return step.equals(other.getStep()) && stepExecution.equals(other.getStepExecution());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return 23*step.hashCode() + 61*stepExecution.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "step=" + step + "; stepExecution=" + stepExecution;
	}

}
