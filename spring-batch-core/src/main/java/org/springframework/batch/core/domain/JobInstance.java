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

import org.springframework.util.Assert;

/**
 * Batch domain object representing a uniquely identifiable job run - it's
 * identity is given by the pair {@link Job} and {@link JobParameters}.
 * JobInstance can be restarted multiple times in case of execution failure and
 * it's lifecycle ends with first successful execution.
 * 
 * Trying to execute an existing JobIntance that has already completed
 * successfully will result in error. Error will be raised also for an attempt
 * to restart a failed JobInstance if the Job ({@link JobInstance#getJob()}) is not
 * restartable.
 * 
 * @see Job
 * @see JobParameters
 * @see JobExecution
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class JobInstance extends Entity {

	private JobParameters jobParameters;

	private Job job;

	private JobExecution lastExecution;

	public JobInstance(Long id, JobParameters jobParameters, Job job) {
		super(id);
		Assert.notNull(job);
		this.jobParameters = jobParameters == null ? new JobParameters()
				: jobParameters;
		this.job = job;
	}

	public void setLastExecution(JobExecution lastExecution) {
		this.lastExecution = lastExecution;
	}

	public JobExecution getLastExecution() {
		return lastExecution;
	}

	/**
	 * @return {@link JobParameters}
	 */
	public JobParameters getJobParameters() {
		return jobParameters;
	}

	/**
	 * @return the job name. (Equivalent to getJob().getName())
	 */
	public String getJobName() {
		return job == null ? null : job.getName();
	}

	public JobExecution createJobExecution() {
		JobExecution newExecution = new JobExecution(this);
		this.setLastExecution(newExecution);
		return newExecution;
	}

	public String toString() {
		return super.toString() + ", JobParameters=[" + jobParameters + "]"
				+ ", Job=[" + job + "]";
	}

	public Job getJob() {
		return job;
	}

}
