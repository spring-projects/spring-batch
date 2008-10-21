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

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
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
import org.springframework.transaction.annotation.Isolation;
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

	/**
	 * @see JobRepository#isJobInstanceExists(String, JobParameters)
	 */
	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return jobInstanceDao.getJobInstance(jobName, jobParameters)!=null;
	}
	
	/**
	 * <p>
	 * Create a {@link JobExecution} based on the passed in {@link Job} and
	 * {@link JobParameters}. However, unique identification of a job can only
	 * come from the database, and therefore must come from JobDao by either
	 * creating a new job instance or finding an existing one, which will ensure
	 * that the id of the job instance is populated with the correct value.
	 * </p>
	 * 
	 * <p>
	 * There are two ways in which the method determines if a job should be
	 * created or an existing one should be returned. The first is
	 * restartability. The {@link Job} restartable property will be checked
	 * first. If it is false, a new job will be created, regardless of whether
	 * or not one exists. If it is true, the {@link JobInstanceDao} will be
	 * checked to determine if the job already exists, if it does, it's steps
	 * will be populated (there must be at least 1) and a new
	 * {@link JobExecution} will be returned. If no job instance is found, a new
	 * one will be created.
	 * </p>
	 * 
	 * <p>
	 * A check is made to see if any job executions are already running, and an
	 * exception will be thrown if one is detected. To detect a running job
	 * execution we use the {@link JobExecutionDao}:
	 * <ol>
	 * <li>First we find all jobs which match the given {@link JobParameters}
	 * and job name</li>
	 * <li>What happens then depends on how many existing job instances we find:
	 * <ul>
	 * <li>If there are none, or the {@link Job} is marked restartable, then we
	 * create a new {@link JobInstance}</li>
	 * <li>If there is more than one and the {@link Job} is not marked as
	 * restartable, it is an error. This could be caused by a job whose
	 * restartable flag has changed to be more strict (true not false)
	 * <em>after</em> it has been executed at least once.</li>
	 * <li>If there is precisely one existing {@link JobInstance} then we check
	 * the {@link JobExecution} instances for that job, and if any of them tells
	 * us it is running (see {@link JobExecution#isRunning()}) then it is an
	 * error.</li>
	 * </ul>
	 * </li>
	 * </ol>
	 * If this method is run in a transaction (as it normally would be) with
	 * isolation level at {@link Isolation#REPEATABLE_READ} or better, then this
	 * method should block if another transaction is already executing it (for
	 * the same {@link JobParameters} and job name). The first transaction to
	 * complete in this scenario obtains a valid {@link JobExecution}, and
	 * others throw {@link JobExecutionAlreadyRunningException} (or timeout).
	 * There are no such guarantees if the {@link JobInstanceDao} and
	 * {@link JobExecutionDao} do not respect the transaction isolation levels
	 * (e.g. if using a non-relational data-store, or if the platform does not
	 * support the higher isolation levels).
	 * </p>
	 * 
	 * @see JobRepository#createJobExecution(String, JobParameters)
	 * 
	 */
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
				if (execution.getStatus() == BatchStatus.COMPLETED) {
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

		return jobExecution;

	}

	/**
	 * Save or Update a JobExecution. A JobExecution is considered one
	 * 'execution' of a particular job. Therefore, it must have it's jobId field
	 * set before it is passed into this method. It also has it's own unique
	 * identifier, because it must be updatable separately. If an id isn't
	 * found, a new JobExecution is created, if one is found, the current row is
	 * updated.
	 * 
	 * @param jobExecution to be stored.
	 * @throws IllegalArgumentException if jobExecution is null.
	 */
	public void update(JobExecution jobExecution) {

		Assert.notNull(jobExecution, "JobExecution cannot be null.");
		Assert.notNull(jobExecution.getJobId(), "JobExecution must have a Job ID set.");
		Assert.notNull(jobExecution.getId(), "JobExecution must be already saved (have an id assigned).");

		jobExecution.setLastUpdated(new Date(System.currentTimeMillis()));
		jobExecutionDao.updateJobExecution(jobExecution);
	}

	/**
	 * Save the {@link StepExecution}.
	 * 
	 * Preconditions: step name must be given and associated
	 * {@link JobExecution} must already be saved (have an id assigned).
	 */
	public void add(StepExecution stepExecution) {
		validateStepExecution(stepExecution);

		stepExecution.setLastUpdated(new Date(System.currentTimeMillis()));
		stepExecutionDao.saveStepExecution(stepExecution);
	}

	/**
	 * Update the {@link StepExecution}.
	 * 
	 * Preconditions: step name must be given and associated
	 * {@link JobExecution} must already be saved (have an id assigned).
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.repository.JobRepository#
	 * persistExecutionContext
	 * (org.springframework.batch.core.domain.StepExecution)
	 */
	public void updateExecutionContext(StepExecution stepExecution) {
		// Until there is an interface change (
		ecDao.persistExecutionContext(stepExecution.getJobExecution());
		ecDao.persistExecutionContext(stepExecution);
	}

	/**
	 * @return the last execution of the step within given job instance
	 */
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		List<JobExecution> jobExecutions = jobExecutionDao.findJobExecutions(jobInstance);
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>(jobExecutions.size());
		for (JobExecution jobExecution : jobExecutions) {
			StepExecution stepExecution = stepExecutionDao.getStepExecution(jobExecution, stepName);
			if (stepExecution != null) {
				stepExecutions.add(stepExecution);
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
			if (stepExecutionDao.getStepExecution(jobExecution, stepName) != null) {
				count++;
			}
		}
		return count;
	}
	
	/* 
	 * Check to determine whether or not the JobExecution that is the parent of the provided
	 * StepExecution has been interrupted.  If, after synchronizing the status with the database,
	 * the status has been updated to STOPPING, then the job has been interrupted.
	 * 
	 * @param stepExecution
	 */
	private void checkForInterruption(StepExecution stepExecution){
		JobExecution jobExecution = stepExecution.getJobExecution();
		jobExecutionDao.synchronizeStatus(jobExecution);
		if(jobExecution.getStatus() == BatchStatus.STOPPING || jobExecution.getStatus() == BatchStatus.PAUSED){
			stepExecution.setTerminateOnly();
		}
	}

	public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobName, jobParameters);
		if (jobInstance == null) {
			return null;
		}
		return jobExecutionDao.getLastJobExecution(jobInstance);
		
	}

}
