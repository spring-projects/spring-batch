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

import org.springframework.batch.item.StreamContext;

/**
 * <p>
 * Batch domain entity representing a step which is sequentially executed by a
 * job. Logically, steps are identified as a function of a job plus each step's
 * name. For example, job 'TestJob' which has 2 steps: "TestStep1" and
 * "TestStep2". The first step can be thought of as identified by
 * "TestJob.TestStep1". In relational terms this may be represented by a foreign
 * key on the Job's ID. Therefore, Each step instance is uniquely identified by
 * it's ID, which is obtained from a JobRepository. Two steps with the same name
 * and same job can be considered the same step.
 * </p>
 *
 * <p>
 * Because each step represents a runnable batch artifact with it's own
 * lifecycle, each step contains status and an execution count. Status
 * represents the status of each step's last execution (such as started,
 * completed, failed, etc) and execution count is the count of executions for
 * this individual step. It should be noted that a restartable job will create a
 * new step instance (the same logical step, with a different ID) for every run.
 * </p>
 *
 * @author Lucas Ward
 * @author Dave Syer
 *
 */
public class StepInstance extends Entity {

	private JobInstance jobInstance;

	private BatchStatus status;

	private StreamContext streamContext = new StreamContext();

	private int stepExecutionCount = 0;

	private String name;

	/**
	 * Package private constructor for Hibernate only 
	 */
	StepInstance() {
		this(null);
	}

	public StepInstance(Long stepId) {
		this(null, null, stepId);
	}

	public StepInstance(JobInstance job, String name) {
		this(job, name, null);
	}
	
	public StepInstance(JobInstance job, String name, Long stepId) {
		setId(stepId);
		this.jobInstance = job;
		this.name = name;
	}

	public int getStepExecutionCount() {
		return stepExecutionCount;
	}

	public void setStepExecutionCount(int stepExecutionCount) {
		this.stepExecutionCount = stepExecutionCount;
	}

	public StreamContext getStreamContext() {
		return streamContext;
	}

	public void setStreamContext(StreamContext streamContext) {
		this.streamContext = streamContext;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public JobInstance getJobInstance() {
		return jobInstance;
	}

	public String getName() {
		return name;
	}

	public Long getJobId() {
		return jobInstance==null ? null : jobInstance.getId();
	}

	// @Override
	public String toString() {
		return super.toString() + ", name=" + name + ", status=" + getStatus() + " in " + jobInstance;
	}

}
