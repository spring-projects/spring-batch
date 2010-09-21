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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * Message handler which uses strategies to convert a Message into a job and a set of job parameters
 * @author Jonas Partner
 * @author Dave Syer
 * 
 */
public class JobLaunchingMessageHandler implements JobLaunchRequestHandler {

	private final JobLauncher jobLauncher;

	/**
	 * @param jobLauncher
	 */
	public JobLaunchingMessageHandler(JobLauncher jobLauncher) {
		super();
		this.jobLauncher = jobLauncher;
	}

	@ServiceActivator
	public JobExecution launch(JobLaunchRequest request) throws JobExecutionException {
		Job job = request.getJob();
		JobParameters jobParameters = request.getJobParameters();

		JobExecution execution = jobLauncher.run(job, jobParameters);
		return execution;
	}

}
