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

import java.util.ArrayList;
import java.util.List;


/**
 * Batch domain object representing a job instance. A job instance is defined as
 * a logical container for steps with unique identification of the unit as a
 * whole. A job can be executed many times with the same instance, usually if it
 * fails and is restarted, or if it is launched on an ad-hoc basis "on demand".
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class JobInstance extends Entity {

	private List stepInstances = new ArrayList();

	private JobIdentifier identifier;
	
	private Job job;

	// TODO declare transient or make the class serializable
	private BatchStatus status;

	private int jobExecutionCount;
	
	public JobInstance(JobIdentifier jobIdentifier){
		this.identifier = jobIdentifier;
	}

	public JobInstance(JobIdentifier identifier, Long id, Job job) {
		super();
		setId(id);
		this.identifier = identifier;
		this.job = job;
	}
	
	public JobInstance(JobIdentifier jobIdentifier, Long id){
		this(jobIdentifier, id, null);
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public List getStepInstances() {
		return stepInstances;
	}

	public void setStepInstances(List stepInstances) {
		this.stepInstances = stepInstances;
	}

	public void addStepInstance(StepInstance stepInstance) {
		this.stepInstances.add(stepInstance);
	}

	public int getJobExecutionCount() {
		return jobExecutionCount;
	}

	public void setJobExecutionCount(int jobExecutionCount) {
		this.jobExecutionCount = jobExecutionCount;
	}

	/**
	 * Public accessor for the identifier property.
	 *
	 * @return the identifier
	 */
	public JobIdentifier getIdentifier() {
		return identifier;
	}

	/**
	 * @return the identifier name if there is one
	 */
	public String getName() {
		return identifier==null ? null : identifier.getName();
	}
	
	public JobExecution createJobExecution() {
		return new JobExecution(this);
	}
	
	public String toString() {
		return super.toString()+", identifier=["+identifier+"]";
	}
	
	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}
}
