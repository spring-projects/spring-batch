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
package org.springframework.batch.integration.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;

/**
 * Encapsulation of a {@link Job} and its {@link JobParameters} forming a request for a job to be launched.
 * 
 * @author Dave Syer
 * 
 */
public class JobLaunchRequest {

	private final Job job;

	private final JobParameters jobParameters;

	/**
	 * @param job
	 * @param jobParameters
	 */
	public JobLaunchRequest(Job job, JobParameters jobParameters) {
		super();
		this.job = job;
		this.jobParameters = jobParameters;
	}

	/**
	 * @return the {@link Job} to be executed
	 */
	public Job getJob() {
		return this.job;
	}

	/**
	 * @return the {@link JobParameters} for this request
	 */
	public JobParameters getJobParameters() {
		return this.jobParameters;
	}

	@Override
	public String toString() {
		return "JobLaunchRequest: " + job.getName() + ", parameters=" + jobParameters;
	}

}
