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

package org.springframework.batch.core.repository.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.Assert;

/**
 * 
 * <p>
 * Implementation of {@link JobRepository} that stores JobInstances,
 * JobExecutions, and StepExecutions using the injected DAOs.
 * <p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 * @see JobRepository
 * @see JobInstanceDao
 * @see JobExecutionDao
 * @see StepExecutionDao
 * 
 */
public class SimpleJobRepository implements JobRepository {

	private static final Log logger = LogFactory.getLog(SimpleJobRepository.class);

	private JobInstanceDao jobInstanceDao;

	private JobExecutionDao jobExecutionDao;

	private StepExecutionDao stepExecutionDao;

	private ExecutionContextDao ecDao;

	/**
	 * Provide default constructor with low visibility in case user wants to use
	 * use aop:proxy-target-class="true" for AOP interceptor.
	 */
	SimpleJobRepository() {
	}

	public SimpleJobRepository(JobInstanceDao jobInstanceDao, JobExecutionDao jobExecutionDao,
			StepExecutionDao stepExecutionDao, ExecutionContextDao ecDao) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
		this.ecDao = ecDao;
	}

	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return jobInstanceDao.getJobInstance(jobName, jobParameters) != null;
	}

	public JobExecution createJobExecution(String jobName, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

		Assert.notNull(jobName, "Job name must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		/*
		 * Find all jobs matching the runtime information.
		 * 
		 * If this method is transactional, and the isolation level is
		 * REPEATABLE_READ or better, another launcher trying to start the same
		 * job in another thread or process will block until this transaction
		 * has finished.
		 */

		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobName, jobParameters);
		ExecutionContext executionContext;

		// existing job instance found
		if (jobInstance != null) {

			List<JobExecution> executions = jobExecutionDao.findJobExecutions(jobInstance);

			// check for running executions and find the last started
			for (JobExecution execution : executions) {
				if (execution.isRunning()) {
					throw new JobExecutionAlreadyRunningException("A job execution for this job is already running: "
							+ jobInstance);
				}
				
				BatchStatus status = execution.getStatus();
				if (status == BatchStatus.COMPLETED || status == BatchStatus.ABANDONED) {
					throw new JobInstanceAlreadyCompleteException(
							"A job instance already exists and is complete for parameters=" + jobParameters
									+ ".  If you want to run this job again, change the parameters.");
				}
			}
			executionContext = ecDao.getExecutionContext(jobExecutionDao.getLastJobExecution(jobInstance));
		}
		else {
			// no job found, create one
			jobInstance = jobInstanceDao.createJobInstance(jobName, jobParameters);
			executionContext = new ExecutionContext();
		}

		JobExecution jobExecution = new JobExecution(jobInstance);
		jobExecution.setExecutionContext(executionContext);
		jobExecution.setLastUpdated(new Date(System.currentTimeMillis()));

		// Save the JobExecution so that it picks up an ID (useful for clients
		// monitoring asynchronous executions):
		jobExecutionDao.saveJobExecution(jobExecution);
		ecDao.saveExecutionContext(jobExecution);

		return jobExecution;

	}

	public void update(JobExecution jobExecution) {

		Assert.notNull(jobExecution, "JobExecution cannot be null.");
		Assert.notNull(jobExecution.getJobId(), "JobExecution must have a Job ID set.");
		Assert.notNull(jobExecution.getId(), "JobExecution must be already saved (have an id assigned).");

		jobExecution.setLastUpdated(new Date(System.currentTimeMillis()));
		jobExecutionDao.updateJobExecution(jobExecution);
	}

	public void add(StepExecution stepExecution) {
		validateStepExecution(stepExecution);

		stepExecution.setLastUpdated(new Date(System.currentTimeMillis()));
		stepExecutionDao.saveStepExecution(stepExecution);
		ecDao.saveExecutionContext(stepExecution);
	}

	public void update(StepExecution stepExecution) {
		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution must already be saved (have an id assigned)");

		stepExecution.setLastUpdated(new Date(System.currentTimeMillis()));
		stepExecutionDao.updateStepExecution(stepExecution);
		checkForInterruption(stepExecution);
	}

	private void validateStepExecution(StepExecution stepExecution) {
		Assert.notNull(stepExecution, "StepExecution cannot be null.");
		Assert.notNull(stepExecution.getStepName(), "StepExecution's step name cannot be null.");
		Assert.notNull(stepExecution.getJobExecutionId(), "StepExecution must belong to persisted JobExecution");
	}

	public void updateExecutionContext(StepExecution stepExecution) {
		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution must already be saved (have an id assigned)");
		ecDao.updateExecutionContext(stepExecution);
	}
	
	public void updateExecutionContext(JobExecution jobExecution) {
		ecDao.updateExecutionContext(jobExecution);
	}

	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		List<JobExecution> jobExecutions = jobExecutionDao.findJobExecutions(jobInstance);
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>(jobExecutions.size());
		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
			for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
				if (stepName.equals(stepExecution.getStepName())) {
					stepExecutions.add(stepExecution);
				}
			}
		}
		StepExecution latest = null;
		for (StepExecution stepExecution : stepExecutions) {
			if (latest == null) {
				latest = stepExecution;
			}
			if (latest.getStartTime().getTime() < stepExecution.getStartTime().getTime()) {
				latest = stepExecution;
			}
		}
		if (latest != null) {
			ExecutionContext executionContext = ecDao.getExecutionContext(latest);
			latest.setExecutionContext(executionContext);
		}
		return latest;
	}

	/**
	 * @return number of executions of the step within given job instance
	 */
	public int getStepExecutionCount(JobInstance jobInstance, String stepName) {
		int count = 0;
		List<JobExecution> jobExecutions = jobExecutionDao.findJobExecutions(jobInstance);
		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
			for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
				if (stepName.equals(stepExecution.getStepName())) {
					count++;
				}
			}
		}
		return count;
	}

	/*
	 * Check to determine whether or not the JobExecution that is the parent of
	 * the provided StepExecution has been interrupted. If, after synchronizing
	 * the status with the database, the status has been updated to STOPPING,
	 * then the job has been interrupted.
	 * 
	 * @param stepExecution
	 */
	private void checkForInterruption(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		jobExecutionDao.synchronizeStatus(jobExecution);
		if (jobExecution.isStopping()) {
			logger.info("Parent JobExecution is stopped, so passing message on to StepExecution");
			stepExecution.setTerminateOnly();
		}
	}

	public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobName, jobParameters);
		if (jobInstance == null) {
			return null;
		}
		JobExecution jobExecution = jobExecutionDao.getLastJobExecution(jobInstance);
	
		if (jobExecution != null) {
			jobExecution.setExecutionContext(ecDao.getExecutionContext(jobExecution));
		}
		return jobExecution;

	}


}
