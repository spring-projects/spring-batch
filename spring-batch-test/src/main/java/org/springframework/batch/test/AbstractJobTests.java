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

package org.springframework.batch.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for testing batch jobs using the SimpleJob implementation. It
 * provides methods for launching a Job, or individual Steps within a Job on
 * their own, allowing for end to end testing of individual steps, without
 * having to run every step in the job. Any test classes inheriting from this
 * class should make sure they are part of an ApplicationContext, which is
 * generally expected to be done as part of the Spring test framework.
 * Furthermore, the ApplicationContext in which it is a part of is expected to
 * have one {@link JobLauncher}, {@link JobRepository}, and a single Job
 * implementation. It should be noted that using any of the methods that don't
 * conain {@link JobParameters} in their signature, will result in one being
 * created with the current system time as a parameter.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AbstractJobTests {

	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobLauncher launcher;

	@Autowired
	private Job job;
	
	@Autowired
	private JobRepository jobRepository;

	public JobRepository getJobRepository() {
		return jobRepository;
	}

	public Job getJob() {
		return job;
	}

	/**
	 * Public getter for the launcher.
	 * 
	 * @return the launcher
	 */
	protected JobLauncher getJobLauncher() {
		return launcher;
	}

	/**
	 * Launch the entire job, including all steps, in order.
	 * 
	 * @return JobExecution, so that the test may validate the exit status
	 * @throws Exception 
	 */
	public JobExecution launchJob() throws Exception {
		return this.launchJob(this.makeUniqueJobParameters());
	}

	/**
	 * Launch the entire job, including all steps, in order.
	 * 
	 * @param jobParameters
	 * @return JobExecution, so that the test may validate the exit status
	 * @throws Exception 
	 */
	public JobExecution launchJob(JobParameters jobParameters) throws Exception {
		return getJobLauncher().run(this.job, jobParameters);
	}

	/**
	 * @return a new JobParameters object containing only a parameter for the
	 * current timestamp, to ensure that the job instance will be unique
	 */
	private JobParameters makeUniqueJobParameters() {
		Map<String, JobParameter> parameters = new HashMap<String, JobParameter>();
		parameters.put("timestamp", new JobParameter(new Date().getTime()));
		return new JobParameters(parameters);
	}
}
