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

package org.springframework.batch.core.explore.support;

import java.util.List;
import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;

/**
 * 
 * <p>
 * Implementation of {@link JobExplorer} using the injected DAOs.
 * <p>
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * 
 * @see JobExplorer
 * @see JobInstanceDao
 * @see JobExecutionDao
 * @see StepExecutionDao
 * @since 2.0
 */
public class SimpleJobExplorer implements JobExplorer {

	private JobInstanceDao jobInstanceDao;

	private JobExecutionDao jobExecutionDao;
	
	private StepExecutionDao stepExecutionDao;

	/**
	 * Provide default constructor with low visibility in case user wants to use
	 * use aop:proxy-target-class="true" for AOP interceptor.
	 */
	SimpleJobExplorer() {
	}

	public SimpleJobExplorer(JobInstanceDao jobInstanceDao, JobExecutionDao jobExecutionDao, StepExecutionDao stepExecutionDao) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.explore.JobExplorer#findJobExecutions(org.springframework.batch.core.JobInstance)
	 */
	public List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		List<JobExecution> executions = jobExecutionDao.findJobExecutions(jobInstance);
		for(JobExecution jobExecution:executions){
			getJobExecutionDependencies(jobExecution);
		}
		return executions;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.explore.JobExplorer#findRunningJobExecutions(java.lang.String)
	 */
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		Set<JobExecution> executions = jobExecutionDao.findRunningJobExecutions(jobName);
		for(JobExecution jobExecution:executions){
			getJobExecutionDependencies(jobExecution);
		}
		return executions;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.explore.JobExplorer#getJobExecution(java.lang.Long)
	 */
	public JobExecution getJobExecution(Long executionId) {
		JobExecution jobExecution = jobExecutionDao.getJobExecution(executionId);
		getJobExecutionDependencies(jobExecution);
		return jobExecution;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.explore.JobExplorer#getJobInstance(java.lang.Long)
	 */
	public JobInstance getJobInstance(Long instanceId) {
		return jobInstanceDao.getJobInstance(instanceId);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.explore.JobExplorer#getLastJobInstances(java.lang.String, int)
	 */
	public List<JobInstance> getLastJobInstances(String jobName, int count) {
		return jobInstanceDao.getLastJobInstances(jobName, count);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.explore.JobExplorer#isJobInstanceExists(java.lang.String, org.springframework.batch.core.JobParameters)
	 */
	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return jobInstanceDao.getJobInstance(jobName, jobParameters)!=null;
	}
	
	/*
	 * Find all dependencies for a JobExecution, including JobInstance (which requires JobParameters)
	 * plus StepExecutions
	 */
	private void getJobExecutionDependencies(JobExecution jobExecution){
		
		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobExecution);
		stepExecutionDao.getStepExecutions(jobExecution);
		jobExecution.setJobInstance(jobInstance);
	}

}
