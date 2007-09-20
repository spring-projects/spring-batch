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

import java.util.Properties;

import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;

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

	private JobInstance job;

	// TODO declare transient or make serializable
	private BatchStatus status;

	private RestartData restartData = new GenericRestartData(new Properties());

	private int stepExecutionCount = 0;

	private StepExecution stepExecution;

	private String name;

	public StepInstance() {
		this(null);
	}

	public StepInstance(Long stepId) {
		setId(stepId);
	}

	public int getStepExecutionCount() {
		return stepExecutionCount;
	}

	public void setStepExecutionCount(int stepExecutionCount) {
		this.stepExecutionCount = stepExecutionCount;
	}

	public RestartData getRestartData() {
		return restartData;
	}

	public void setRestartData(RestartData restartData) {
		this.restartData = restartData;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public void setJob(JobInstance job) {
		this.job = job;
	}

	public JobInstance getJob() {
		return job;
	}

	public StepExecution getStepExecution() {
		return stepExecution;
	}

	public void setStepExecution(StepExecution stepInstance) {
		this.stepExecution = stepInstance;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Long getJobId() {
		return job==null ? null : job.getId();
	}

	// @Override
	public String toString() {
		return super.toString() + ", name=" + name + ", status=" + getStatus() + " in " + job;
	}

}
